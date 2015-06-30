/*
 * Copyright (c) 2015, Kieron Wilkinson
 */
package org.softpres.donkeysql;

import org.junit.Before;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.StrictAssertions.assertThat;

/**
 * Unit tests for {@link ResultSetIterator}.
 */
public class ResultSetIteratorTest {

  private DataSource dataSource;
  private RowMapper<Integer> intMapper;

  @Before
  public void setup() throws Exception {
    dataSource = TestDB.createPopulatedDataSource(5);
    intMapper = resultSet -> resultSet.getInt("key");
  }

  @Test
  public void shouldIndicateNextItemWithSingleResult() throws SQLException {
    String sql = "SELECT 1";

    with(sql, resultSet -> {
      ResultSetIterator<Integer> iterator = new ResultSetIterator<>(resultSet, intMapper);

      assertThat(iterator.hasNext()).isTrue();
    });
  }


  @Test
  public void hasNextCanBeCalledMultipleTimesWithSingleResult() throws SQLException {
    String sql = "SELECT 1";

    with(sql, resultSet -> {
      ResultSetIterator<Integer> iterator = new ResultSetIterator<>(resultSet, intMapper);

      assertThat(iterator.hasNext()).isTrue();
      assertThat(iterator.hasNext()).isTrue();
      assertThat(iterator.hasNext()).isTrue();
    });
  }

  @Test
  public void nextItemCanBeTakenWithoutCallToHasNext() throws SQLException {
    String sql = "SELECT 42";

    with(sql, resultSet -> {
      ResultSetIterator<Integer> iterator = new ResultSetIterator<>(resultSet, rs -> rs.getInt(1));

      assertThat(iterator.next()).isEqualTo(42);
    });
  }

  @Test
  public void multipleItemsCanBeTakenWithoutCallToHasNext() throws SQLException {
    String sql = "SELECT key FROM data";

    with(sql, resultSet -> {
      ResultSetIterator<Integer> iterator = new ResultSetIterator<>(resultSet, intMapper);

      assertThat(iterator.next()).isEqualTo(1);
      assertThat(iterator.next()).isEqualTo(2);
      assertThat(iterator.next()).isEqualTo(3);
    });
  }

  @Test
  public void hasNextAndNextWorkInSynchrony() throws SQLException {
    String sql = "SELECT key FROM data WHERE key <= 2";

    with(sql, resultSet -> {
      ResultSetIterator<Integer> iterator = new ResultSetIterator<>(resultSet, intMapper);

      assertThat(iterator.hasNext()).isTrue();
      assertThat(iterator.hasNext()).isTrue();
      assertThat(iterator.next()).isEqualTo(1);
      assertThat(iterator.hasNext()).isTrue();
      assertThat(iterator.hasNext()).isTrue();
      assertThat(iterator.next()).isEqualTo(2);
      assertThat(iterator.hasNext()).isFalse();
    });
  }

  @Test
  public void resultSetClosedAfterLastResultConsumed() throws SQLException {
    String sql = "SELECT 1";

    with(sql, resultSet -> {
      ResultSetIterator<Integer> iterator = new ResultSetIterator<>(resultSet, rs -> rs.getInt(1));

      iterator.hasNext();
      assertThat(resultSet.isClosed()).isFalse();

      iterator.next();
      assertThat(resultSet.isClosed()).isFalse();

      iterator.hasNext();
      assertThat(resultSet.isClosed()).isTrue();
    });
  }

  @Test
  public void extractSingleObject() throws SQLException {
    String sql = "SELECT key,value FROM data WHERE key = ?";

    withStatement(sql, statement -> {
      statement.setInt(1, 3);
      ResultSet resultSet = statement.executeQuery();

      RowMapper<KeyValue> mapper = rs -> new KeyValue(rs.getInt("key"), rs.getString("value"));
      ResultSetIterator<KeyValue> iterator = new ResultSetIterator<>(resultSet, mapper);

      KeyValue entry = iterator.next();

      assertThat(entry.key).isEqualTo(3);
      assertThat(entry.value).isEqualTo("3");
    });
  }

  private void with(String sql, SQLConsumer<ResultSet> consumer) throws SQLException {
    withStatement(sql, statement -> {
      ResultSet resultSet = statement.executeQuery();
      consumer.apply(resultSet);
    });
  }

  private void withStatement(String sql, SQLConsumer<PreparedStatement> consumer) throws SQLException {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

      consumer.apply(statement);
    }
  }

  private interface SQLConsumer<T> {
    void apply(T t) throws SQLException;
  }

  private static class KeyValue {
    int key;
    String value;

    public KeyValue(int key, String value) {
      this.key = key;
      this.value = value;
    }
  }

}

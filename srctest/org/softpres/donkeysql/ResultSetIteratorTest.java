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

import static org.assertj.core.api.StrictAssertions.assertThat;

/**
 * Unit tests for {@link ResultSetIterator}.
 */
public class ResultSetIteratorTest {

  private DataSource dataSource;

  @Before
  public void setup() throws Exception {
    dataSource = TestDB.createPopulatedDataSource(5);
  }

  @Test
  public void shouldIndicateNextItemWithSingleResult() throws SQLException {
    String sql = "SELECT 1";

    with(sql, resultSet -> {
      ResultSetIterator iterator = new ResultSetIterator(resultSet);

      assertThat(iterator.hasNext()).isTrue();
    });
  }


  @Test
  public void hasNextCanBeCalledMultipleTimesWithSingleResult() throws SQLException {
    String sql = "SELECT 1";

    with(sql, resultSet -> {
      ResultSetIterator iterator = new ResultSetIterator(resultSet);

      assertThat(iterator.hasNext()).isTrue();
      assertThat(iterator.hasNext()).isTrue();
      assertThat(iterator.hasNext()).isTrue();
    });
  }

  @Test
  public void nextItemCanBeTakenWithoutCallToHasNext() throws SQLException {
    String sql = "SELECT 42";

    with(sql, resultSet -> {
      ResultSetIterator iterator = new ResultSetIterator(resultSet);

      assertThat(iterator.next()).isEqualTo(42);
    });
  }

  @Test
  public void multipleItemsCanBeTakenWithoutCallToHasNext() throws SQLException {
    String sql = "SELECT key FROM data";

    with(sql, resultSet -> {
      ResultSetIterator iterator = new ResultSetIterator(resultSet);

      assertThat(iterator.next()).isEqualTo(1);
      assertThat(iterator.next()).isEqualTo(2);
      assertThat(iterator.next()).isEqualTo(3);
    });
  }

  @Test
  public void hasNextAndNextWorkInSynchrony() throws SQLException {
    String sql = "SELECT key FROM data WHERE key <= 2";

    with(sql, resultSet -> {
      ResultSetIterator iterator = new ResultSetIterator(resultSet);

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
      ResultSetIterator iterator = new ResultSetIterator(resultSet);

      iterator.hasNext();
      assertThat(resultSet.isClosed()).isFalse();

      iterator.next();
      assertThat(resultSet.isClosed()).isFalse();

      iterator.hasNext();
      assertThat(resultSet.isClosed()).isTrue();
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

}

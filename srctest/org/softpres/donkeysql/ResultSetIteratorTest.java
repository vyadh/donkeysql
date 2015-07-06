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
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ResultSetIterator}.
 */
public class ResultSetIteratorTest {

  private DataSource dataSource;
  private RowMapper<Integer> intMapper;

  @Before
  public void setup() throws Exception {
    dataSource = TestDB.createPopulatedDataSource();
    intMapper = resultSet -> resultSet.getInt("id");
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
    String sql = "SELECT id FROM animals";

    with(sql, resultSet -> {
      ResultSetIterator<Integer> iterator = new ResultSetIterator<>(resultSet, intMapper);

      assertThat(iterator.next()).isEqualTo(1);
      assertThat(iterator.next()).isEqualTo(2);
      assertThat(iterator.next()).isEqualTo(3);
    });
  }

  @Test
  public void hasNextAndNextWorkInSynchrony() throws SQLException {
    String sql = "SELECT id FROM animals WHERE id <= 2";

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
  public void onCloseIsCalledAtEndOfStream() throws SQLException {
    String sql = "SELECT 1";

    with(sql, resultSet -> {
      AtomicBoolean closed = new AtomicBoolean(false);
      ResultSetIterator<Integer> iterator = new ResultSetIterator<>(resultSet, rs -> rs.getInt(1))
            .onClose(() -> closed.set(true));

      iterator.hasNext();
      assertThat(closed.get()).isFalse();

      iterator.next();
      assertThat(closed.get()).isFalse();

      iterator.hasNext();
      assertThat(closed.get()).isTrue();
    });
  }

  @Test
  public void extractSingleObject() throws SQLException {
    String sql = "SELECT name,legs FROM animals WHERE name = ?";

    withStatement(sql, statement -> {
      statement.setString(1, "dog");
      ResultSet resultSet = statement.executeQuery();

      RowMapper<Animal> mapper = rs -> new Animal(rs.getString("name"), rs.getInt("legs"));
      ResultSetIterator<Animal> iterator = new ResultSetIterator<>(resultSet, mapper);

      Animal entry = iterator.next();

      assertThat(entry.name).isEqualTo("dog");
      assertThat(entry.legs).isEqualTo(4);
    });
  }

  @Test
  public void stream() throws SQLException {
    String sql = "SELECT id FROM animals WHERE id <= 5";

    with(sql, resultSet -> {
      ResultSetIterator<Integer> iterator = new ResultSetIterator<>(resultSet, intMapper);

      List<Integer> result = iterator.stream().collect(toList());

      assertThat(result).containsOnly(1, 2, 3, 4, 5);
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

  private static class Animal {
    String name;
    int legs;

    public Animal(String name, int legs) {
      this.name = name;
      this.legs = legs;
    }
  }

}

/*
 * Copyright (c) 2015, Kieron Wilkinson
 */
package org.softpres.donkeysql;

import org.junit.Before;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Unit tests for {@link DB}.
 */
public class DBTest {

  private DataSource dataSource;

  @Before
  public void setup() throws Exception {
    dataSource = TestDB.createPopulatedDataSource();
  }

  @Test
  public void queryWithNoParameters() throws SQLException {
    try (Connection connection = dataSource.getConnection()) {
      List<Integer> results = DB.with(connection)
            .query("select id from animals")
            .map(resultSet -> resultSet.getInt("id"))
            .stream()
            .collect(toList());

      assertThat(results).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }
  }

  @Test
  public void queryWithParameters() throws SQLException {
    try (Connection connection = dataSource.getConnection()) {
      List<Integer> results = DB.with(connection)
            .query("select id from animals where id <= ? or id >= ?")
            .params(2, 9)
            .map(resultSet -> resultSet.getInt("id"))
            .stream()
            .collect(toList());

      assertThat(results).containsExactly(1, 2, 9, 10);
    }
  }

  @Test
  public void queryWithTwoParameterStylesIsUnsupported() throws SQLException {
    try (Connection connection = dataSource.getConnection()) {
      Throwable throwable = catchThrowable(() -> DB.with(connection)
            .query("select id from animals where id < :id")
            .param("id", 5)
            .params(1, 5)
            .map(resultSet -> resultSet.getInt("id"))
            .stream()
            .collect(toList()));

      assertThat(throwable).hasMessage(
            "Unsupported parameter configuration (uses of named and anon parameters)");
    }
  }

  @Test
  public void queryWithUnspecifiedParameter() throws SQLException {
    try (Connection connection = dataSource.getConnection()) {
      Throwable throwable = catchThrowable(() -> DB.with(connection)
            .query("select id from animals where name = :name")
            .map(resultSet -> resultSet.getString("id"))
            .stream()
            .collect(toList()));

      assertThat(throwable).hasMessage("Unspecified parameter: name");
    }
  }

  @Test
  public void queryWithNamedParameters() throws SQLException {
    try (Connection connection = dataSource.getConnection()) {
      List<String> results = DB.with(connection)
            .query("select name from animals where id > :five and name like :name")
            .param("five", 5)
            .param("name", "b%")
            .map(resultSet -> resultSet.getString("name"))
            .stream()
            .collect(toList());

      assertThat(results).containsOnly("beetle");
    }
  }

  @Test
  public void queryWithMapping() throws SQLException {
    try (Connection connection = dataSource.getConnection()) {
      List<String> results = DB.with(connection)
            .query("select id, name from animals where name like ?")
            .params("%or%")
            .map(resultSet -> resultSet.getInt("id") + "=" + resultSet.getString("name"))
            .stream()
            .collect(toList());

      assertThat(results).containsOnly("6=worm", "9=horse");
    }
  }

  @Test
  public void nonSuppledParameters() throws SQLException {
    try (Connection connection = dataSource.getConnection()) {
      Throwable error = catchThrowable(() -> DB.with(connection)
            .query("select id from animals where id = ? or id = ?")
            .params(1) // Only one
            .map(resultSet -> resultSet.getInt("id")));

      assertThat(error).hasCauseInstanceOf(SQLException.class);
    }
  }

  @Test
  public void tooManyParameters() throws SQLException {
    try (Connection connection = dataSource.getConnection()) {
      Throwable error = catchThrowable(() -> DB.with(connection)
            .query("select id from animals where id = ? or id = ?")
            .params(1, 2, 3) // Extra one
            .map(resultSet -> resultSet.getInt("id")));

      assertThat(error).hasCauseInstanceOf(SQLException.class);
    }
  }

}

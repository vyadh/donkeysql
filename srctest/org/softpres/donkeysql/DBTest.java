/*
 * Copyright (c) 2015, Kieron Wilkinson
 */
package org.softpres.donkeysql;

import org.junit.Before;
import org.junit.Test;

import javax.sql.DataSource;
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
    List<Integer> results = DB.with(dataSource)
          .query("SELECT id FROM animals")
          .map(resultSet -> resultSet.getInt("id"))
          .stream()
          .collect(toList());

    assertThat(results).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
  }

  @Test
  public void queryWithParameters() throws SQLException {
    List<Integer> results = DB.with(dataSource)
          .query("SELECT id FROM animals WHERE id <= ? OR id >= ?")
          .params(2, 9)
          .map(resultSet -> resultSet.getInt("id"))
          .stream()
          .collect(toList());

    assertThat(results).containsExactly(1, 2, 9, 10);
  }

  @Test
  public void queryWithTwoParameterStylesIsUnsupported() throws SQLException {
    Throwable throwable = catchThrowable(() -> DB.with(dataSource)
          .query("SELECT id FROM animals WHERE id < :id")
          .param("id", 5)
          .params(1, 5)
          .map(resultSet -> resultSet.getInt("id"))
          .stream()
          .collect(toList()));

    assertThat(throwable).hasMessage(
          "Unsupported parameter configuration (uses of named and anon parameters)");
  }

  @Test
  public void queryWithUnspecifiedParameter() throws SQLException {
    Throwable throwable = catchThrowable(() -> DB.with(dataSource)
          .query("SELECT id FROM animals WHERE name = :name")
          .map(resultSet -> resultSet.getString("id"))
          .stream()
          .collect(toList()));

    assertThat(throwable).hasMessage("Unspecified parameter: name");
  }

  @Test
  public void queryWithNamedParameters() throws SQLException {
    List<String> results = DB.with(dataSource)
          .query("SELECT name FROM animals WHERE id > :five AND name LIKE :name")
          .param("five", 5)
          .param("name", "b%")
          .map(resultSet -> resultSet.getString("name"))
          .stream()
          .collect(toList());

    assertThat(results).containsOnly("beetle");
  }

  @Test
  public void queryWithMapping() throws SQLException {
    List<String> results = DB.with(dataSource)
          .query("SELECT id, name FROM animals WHERE name LIKE ?")
          .params("%or%")
          .map(resultSet -> resultSet.getInt("id") + "=" + resultSet.getString("name"))
          .stream()
          .collect(toList());

    assertThat(results).containsOnly("6=worm", "9=horse");
  }

  @Test
  public void nonSuppledParameters() throws SQLException {
    Throwable error = catchThrowable(() -> DB.with(dataSource)
          .query("SELECT id FROM animals WHERE id = ? OR id = ?")
          .params(1) // Only one
          .map(resultSet -> resultSet.getInt("id")));

    assertThat(error).hasCauseInstanceOf(SQLException.class);
  }

  @Test
  public void tooManyParameters() throws SQLException {
    Throwable error = catchThrowable(() -> DB.with(dataSource)
          .query("SELECT id FROM animals WHERE id = ? OR id = ?")
          .params(1, 2, 3) // Extra one
          .map(resultSet -> resultSet.getInt("id")));

    assertThat(error).hasCauseInstanceOf(SQLException.class);
  }

}

/*
 * Copyright (c) 2015, Kieron Wilkinson
 */
package org.softpres.donkeysql;

import org.junit.Before;
import org.junit.Test;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Unit tests for {@link DB}.
 */
public class DBTest {

  private DataSource dataSource;

  @Before
  public void populateDataSource() throws Exception {
    dataSource = TestDB.createPopulatedDataSource();
  }

  @Test
  public void queryWithNoParameters() {
    List<Integer> results = DB.with(dataSource)
          .query("SELECT id FROM animals")
          .map(resultSet -> resultSet.getInt("id"))
          .execute()
          .collect(toList());

    assertThat(results).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
  }

  @Test
  public void queryWithParameters() {
    List<Integer> results = DB.with(dataSource)
          .query("SELECT id FROM animals WHERE id <= ? OR id >= ?")
          .params(2, 9)
          .map(resultSet -> resultSet.getInt("id"))
          .execute()
          .collect(toList());

    assertThat(results).containsExactly(1, 2, 9, 10);
  }

  @Test
  public void queryWithUnspecifiedParameter() {
    Throwable throwable = catchThrowable(() -> DB.with(dataSource)
          .query("SELECT id FROM animals WHERE name = :name")
          .map(resultSet -> resultSet.getString("id"))
          .execute());

    assertThat(throwable).hasMessageContaining("Syntax error in SQL statement");
  }

  @Test
  public void queryWithUnusedParameter() {
    List<Integer> ids = DB.with(dataSource)
          .query("SELECT id FROM animals WHERE id = :first OR id = :second")
          .param("first", 1)
          .param("second", 2)
          .param("third", 3) // Extra one doesn't stop query working
          .map(resultSet -> resultSet.getInt("id"))
          .execute()
          .collect(toList());

    assertThat(ids).contains(1, 2);
  }

  @Test
  public void queryWithNamedParameters() {
    List<String> results = DB.with(dataSource)
          .query("SELECT name FROM animals WHERE id > :five AND name LIKE :name")
          .param("five", 5)
          .param("name", "b%")
          .map(resultSet -> resultSet.getString("name"))
          .execute()
          .collect(toList());

    assertThat(results).containsOnly("beetle");
  }

  @Test
  public void queryWithConcatMapping() {
    List<String> results = DB.with(dataSource)
          .query("SELECT id, name FROM animals WHERE name LIKE ?")
          .params("%or%")
          .map(resultSet -> resultSet.getInt("id") + "=" + resultSet.getString("name"))
          .execute()
          .collect(toList());

    assertThat(results).containsOnly("6=worm", "9=horse");
  }

  @Test
  public void nonSuppliedParameters() {
    Throwable error = catchThrowable(() -> DB.with(dataSource)
          .query("SELECT id FROM animals WHERE id = ? OR id = ?")
          .params(1) // Only one
          .map(resultSet -> resultSet.getInt("id"))
          .execute());

    assertThat(error)
          .isInstanceOf(UncheckedSQLException.class)
          .hasMessageStartingWith("Parameters supplied do not correspond to SQL statement");
  }

  @Test
  public void tooManyParameters() {
    Throwable error = catchThrowable(() -> DB.with(dataSource)
          .query("SELECT id FROM animals WHERE id = ? OR id = ?")
          .params(1, 2, 3) // Extra one
          .map(resultSet -> resultSet.getInt("id"))
          .execute());

    assertThat(error)
          .isInstanceOf(UncheckedSQLException.class)
          .hasMessageStartingWith("Parameters supplied do not correspond to SQL statement");
  }

  @Test
  public void parameterMismatchCheckPassesAsOnlyCountsNonQuoted() {
    Stream<String> names = DB.with(dataSource)
          .query("SELECT name FROM animals WHERE name = ? OR name LIKE '?%'")
          .params("cat")
          .map(resultSet -> resultSet.getString("name"))
          .execute();

    assertThat(names).containsOnly("cat");
  }

  @Test
  public void queryWithIterableNamedParameters() {
    Stream<String> names = DB.with(dataSource)
          .query("SELECT name FROM animals WHERE legs IN (:legs) OR name IN (:names)")
          .param("legs", Arrays.asList(6, 8))
          .param("names", Arrays.asList("dog", "cat"))
          .map(resultSet -> resultSet.getString("name"))
          .execute();

    assertThat(names).containsOnly("ant", "beetle", "spider", "dog", "cat");
  }

}

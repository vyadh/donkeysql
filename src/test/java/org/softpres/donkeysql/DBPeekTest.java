/*
 * Copyright (c) 2017, Kieron Wilkinson
 */
package org.softpres.donkeysql;

import org.junit.Before;
import org.junit.Test;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for peeking at the SQL for different kinds of queries
 * (no params, indexed params, named params).
 */
public class DBPeekTest {

  private DataSource dataSource;
  private Capture capture;

  @Before
  public void populateDataSource() throws Exception {
    dataSource = TestDB.createPopulatedDataSource();
    capture = new Capture();
  }

  @Test
  public void canExecuteAfterPeek() {
    Stream<Integer> legs = DB.with(dataSource)
          .query("SELECT legs FROM animals WHERE name = 'dog'")
          .map(resultSet -> resultSet.getInt("legs"))
          .peek(sql -> { })
          .execute();

    assertThat(legs).containsOnly(4);
  }

  @Test
  public void queryWithNoParameters() {
    String sql = "SELECT name FROM animals";

    DB.with(dataSource)
          .query(sql)
          .map(resultSet -> resultSet.getString("name"))
          .peek(capture::set);

    assertThat(capture.sql).isEqualTo(sql);
  }

  @Test
  public void queryWithParameters() {
    DB.with(dataSource)
          .query("SELECT name FROM animals WHERE legs < ? OR name = ?")
          .params(2, "dog")
          .map(resultSet -> resultSet.getString("name"))
          .peek(capture::set);

    assertThat(capture.sql)
          .isEqualTo("SELECT name FROM animals WHERE legs < 2 OR name = 'dog'");
  }

  @Test
  public void queryWithNamedParameters() {
    DB.with(dataSource)
          .query("SELECT name FROM animals WHERE legs > :five AND name LIKE :name")
          .param("five", 5)
          .param("name", "b%")
          .map(resultSet -> resultSet.getString("name"))
          .peek(capture::set);

    assertThat(capture.sql)
          .isEqualTo("SELECT name FROM animals WHERE legs > 5 AND name LIKE 'b%'");
  }

  @Test
  public void queryWithIterableNamedParameters() {
    DB.with(dataSource)
          .query("SELECT name FROM animals WHERE legs IN (:legs) OR name IN (:names)")
          .param("legs", Arrays.asList(6, 8))
          .param("names", Arrays.asList("dog", "cat", "bird"))
          .map(resultSet -> resultSet.getString("name"))
          .peek(capture::set);

    assertThat(capture.sql)
          .isEqualTo("SELECT name FROM animals WHERE legs IN (6,8) OR name IN ('dog','cat','bird')");
  }

  @Test
  public void queryWithIterableOptimisedNamedParameters() {
    DB.with(dataSource)
          .query("SELECT name FROM animals WHERE name IN (@names)")
          .param("names", Arrays.asList("dog", "cat", "bird"))
          .map(resultSet -> resultSet.getString("name"))
          .peek(capture::set);

    assertThat(capture.sql)
          .isEqualTo("SELECT name FROM animals WHERE name IN ('dog','cat','bird','bird')");
  }

  private static class Capture {
    private String sql;

    void set(String sql) {
      this.sql = sql;
    }
  }

}

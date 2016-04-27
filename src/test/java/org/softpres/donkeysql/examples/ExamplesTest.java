/*
 * Copyright (c) 2016, Kieron Wilkinson
 */

package org.softpres.donkeysql.examples;

import org.junit.Before;
import org.junit.Test;
import org.softpres.donkeysql.DB;
import org.softpres.donkeysql.TestDB;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contains runnable examples for README.md.
 */
public class ExamplesTest {

  private DataSource dataSource;

  @Before
  public void setup() throws Exception {
    dataSource = TestDB.createPopulatedDataSource();
  }

  @Test
  public void queryWithNoParameters() throws SQLException {
    Stream<Animal> results = DB.with(dataSource)
          .query("SELECT name, legs FROM animals WHERE legs >= 5")
          .map(resultSet -> new Animal(
                resultSet.getString("name"),
                resultSet.getInt("legs")
          ))
          .execute();

    assertThat(results.collect(toList())).containsExactly(
          new Animal("spider", 8),
          new Animal("ant", 6),
          new Animal("beetle", 6)
    );
  }

  @Test
  public void queryWithPreparedStatementStyleParameters() throws SQLException {
    Stream<Animal> results = DB.with(dataSource)
          .query("SELECT name, legs FROM animals WHERE legs >= ? AND name LIKE ?")
          .params(5, "s%")
          .map(resultSet -> new Animal(
                resultSet.getString("name"),
                resultSet.getInt("legs")
          ))
          .execute();

    assertThat(results.collect(toList())).containsExactly(new Animal("spider", 8));
  }

  @Test
  public void queryWithNamedParameters() throws SQLException {
    Stream<Animal> results = DB.with(dataSource)
          .query("SELECT name, legs FROM animals WHERE legs >= :minLegs AND name LIKE :name")
          .param("minLegs", 5)
          .param("name", "s%")
          .map(resultSet -> new Animal(
                resultSet.getString("name"),
                resultSet.getInt("legs")
          ))
          .execute();

    assertThat(results.collect(toList())).containsExactly(new Animal("spider", 8));
  }

  @Test
  public void queryWithExplicitConnection() throws SQLException {
    try (Connection connection = dataSource.getConnection()) {
      Stream<String> results = DB.with(connection)
            .query("SELECT name FROM animals WHERE legs >= 5")
            .map(resultSet -> resultSet.getString("name"))
            .execute();

      assertThat(results.collect(toList())).containsExactly("spider", "ant", "beetle");
    }
  }

}

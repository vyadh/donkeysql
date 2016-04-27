/*
 * Copyright (c) 2015, Kieron Wilkinson
 */
package org.softpres.donkeysql;

import org.h2.jdbcx.JdbcConnectionPool;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Utility methods for DB tests against simple key-value style database.
 */
public class TestDB {

  public static DataSource createPopulatedDataSource() throws SQLException {
    DataSource dataSource = createDataSource();

    try (Connection connection = dataSource.getConnection()) {
      applySchema(connection);
      insertTestData(connection);
    }

    return dataSource;
  }

  private static DataSource createDataSource() throws SQLException {
    return JdbcConnectionPool.create("jdbc:h2:mem:", "user", "pass");
  }

  private static void applySchema(Connection connection) throws SQLException {
//    String sql = "CREATE TABLE data (key INTEGER PRIMARY KEY, value VARCHAR(100))";
    String sql = "CREATE TABLE animals (" +
          "id INTEGER PRIMARY KEY, " +
          "name VARCHAR(20), " +
          "legs INTEGER)";

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.executeUpdate();
    }
  }

  private static void insertTestData(Connection connection) throws SQLException {
    String sql = "INSERT INTO animals VALUES (?, ?, ?)";

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      insert(statement, 1, "dog", 4);
      insert(statement, 2, "cat", 4);
      insert(statement, 3, "mouse", 4);
      insert(statement, 4, "bird", 2);
      insert(statement, 5, "fish", 0);
      insert(statement, 6, "worm", 0);
      insert(statement, 7, "spider", 8);
      insert(statement, 8, "ant", 6);
      insert(statement, 9, "horse", 4);
      insert(statement, 10, "beetle", 6);
    }
  }

  private static void insert(PreparedStatement statement, int id, String name, int legs) throws SQLException {
    statement.setInt(1, id);
    statement.setString(2, name);
    statement.setInt(3, legs);
    statement.executeUpdate();
  }

}

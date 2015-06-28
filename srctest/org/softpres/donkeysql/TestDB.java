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
      insertTestData(connection, 100);
    }

    return dataSource;
  }

  private static DataSource createDataSource() throws SQLException {
    return JdbcConnectionPool.create("jdbc:h2:mem:test", "user", "pass");
  }

  private static void applySchema(Connection connection) throws SQLException {
    String sql = "CREATE TABLE data (key INTEGER PRIMARY KEY, value VARCHAR(100))";

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.executeUpdate();
    }
  }

  private static void insertTestData(Connection connection, int rows) throws SQLException {
    String sql = "INSERT INTO data VALUES (?, ?)";

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      for (int i = 0; i <= rows; i++) {
        statement.setInt(1, i);
        statement.setString(2, Integer.toString(i));
        statement.executeUpdate();
      }
    }
  }

}

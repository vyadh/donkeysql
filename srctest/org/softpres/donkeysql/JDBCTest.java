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
 * Unit test for performing basic JDBC operations.
 */
public class JDBCTest {

  private DataSource dataSource;

  @Before
  public void createDatabase() throws SQLException {
    dataSource = TestDB.createPopulatedDataSource();
  }

  @Test
  public void countFromOneHundredEntries() throws SQLException {
    try (Connection connection = dataSource.getConnection()) {
      String query = "SELECT count(1) as size FROM data WHERE key > ?";

      try (PreparedStatement statement = connection.prepareStatement(query)) {
        statement.setInt(1, 40);
        ResultSet resultSet = statement.executeQuery();

        resultSet.next();
        assertThat(resultSet.getInt("size")).isEqualTo(60);
      }
    }
  }

}

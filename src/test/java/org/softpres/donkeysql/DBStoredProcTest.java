/*
 * Copyright (c) 2017, Kieron Wilkinson
 */
package org.softpres.donkeysql;

import org.junit.Before;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DB}.
 */
public class DBStoredProcTest {

  private DataSource dataSource;

  @Before
  public void populateDataSource() throws Exception {
    dataSource = TestDB.createPopulatedDataSource();
  }

  @Test
  public void callWithIndexedParameters() {
    createStoredProc("concatSelf");

    Stream<String> results = DB.with(dataSource)
          .query("{call concatSelf(?)}")
          .params("/abc123")
          .map(resultSet -> resultSet.getString(1))
          .execute();

    assertThat(results).containsExactly("/abc123/abc123");
  }

  @Test
  public void callWithNamedParameters() {
    createStoredProc("plus");

    Stream<Integer> results = DB.with(dataSource)
          .query("{call plus(:first, :second)}")
          .param("first", 11)
          .param("second", 31)
          .map(resultSet -> resultSet.getInt(1))
          .execute();

    assertThat(results).containsExactly(42);
  }

  private void createStoredProc(String name) {
    try (
          Connection connection = dataSource.getConnection();
          Statement statement = connection.createStatement()
    ) {
      createStoredProc(statement, name);
    } catch (SQLException e) {
      throw new UncheckedSQLException(e);
    }
  }

  private void createStoredProc(Statement statement, String name) throws SQLException {
    statement.execute("CREATE ALIAS " + name + " FOR \"" +
          StoredProc.class.getName() +
          "." + name + "\"");
  }

  @SuppressWarnings("unused") // Reflected by H2
  public static class StoredProc {
    public static String concatSelf(String text) {
      return text + text;
    }
    public static int plus(int a, int b) {
      return a + b;
    }
  }

}

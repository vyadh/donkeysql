/*
 * Copyright (c) 2015, Kieron Wilkinson
 */
package org.softpres.donkeysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * Database DSL.
 */
public class DB {

  public static DBConnection with(Connection connection) {
    return new DBConnection(connection);
  }

  public static class DBConnection {
    private final Connection connection;

    public DBConnection(Connection connection) {
      this.connection = connection;
    }

    public DBQuery query(String sql) {
      return new DBQuery(connection, sql);
    }
  }

  public static class DBQuery {
    private final Connection connection;
    private final String sql;
    private Object[] params;

    public DBQuery(Connection connection, String sql) {
      this.connection = connection;
      this.sql = sql;
      this.params = new Object[0];
    }

    public DBQuery params(Object... params) {
      this.params = params;
      return this;
    }

    public <T> ResultSetIterator<T> map(RowMapper<T> mapper) {
      try {

        PreparedStatement statement = connection.prepareStatement(sql);
        applyParameters(statement);
        ResultSet resultSet = statement.executeQuery();

        return new ResultSetIterator<>(resultSet, mapper)
              .onClose(statement::close);

      } catch (SQLException e) {
        throw new UncheckedSQLException(e);
      }
    }

    private void applyParameters(PreparedStatement statement) throws SQLException {
      if (sql.chars().filter(c -> c == '?').count() != params.length) {
        throw new SQLException(
              "Parameters supplied do not correspond to SQL statement: " +
                    sql + ' ' + Arrays.toString(params));
      }

      for (int i = 0; i < params.length; i++) {
        statement.setObject(i+1, params[i]);
      }
    }
  }

}

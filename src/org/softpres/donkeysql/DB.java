/*
 * Copyright (c) 2015, Kieron Wilkinson
 */
package org.softpres.donkeysql;

import org.assertj.core.util.Maps;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Database DSL.
 */
public class DB {

  public static DBConnection with(Connection connection) {
    return new DBConnection(connection, false);
  }

  public static DBConnection with(DataSource dataSource) {
    try {
      Connection connection = dataSource.getConnection();
      return new DBConnection(connection, true);
    } catch (SQLException e) {
      throw new UncheckedSQLException(e);
    }
  }

  public static class DBConnection {
    private final Connection connection;
    private final boolean autoClose;

    DBConnection(Connection connection, boolean autoClose) {
      this.connection = connection;
      this.autoClose = autoClose;
    }

    public DBQuery query(String sql) {
      return new DBQuery(connection, autoClose, sql);
    }
  }

  public static class DBQuery {
    private final Connection connection;
    private final boolean autoClose;
    private final String sql;
    private final Map<String, Object> namedParams;
    private Object[] params;

    DBQuery(Connection connection, boolean autoClose, String sql) {
      this.connection = connection;
      this.autoClose = autoClose;
      this.sql = sql;
      this.namedParams = Maps.newHashMap();
      this.params = new Object[0];
    }

    public DBQuery params(Object... params) {
      this.params = params;
      return this;
    }

    public DBQuery param(String name, Object value) {
      namedParams.put(name, value);
      return this;
    }

    public <T> ResultSetIterator<T> map(RowMapper<T> mapper) {
      try {

        String normalisedSQL = StringParameters.normalise(sql);
        PreparedStatement statement = connection.prepareStatement(normalisedSQL);
        applyParameters(statement);
        ResultSet resultSet = statement.executeQuery();

        return new ResultSetIterator<>(resultSet, mapper)
              .onClose(createAutoClosable(statement));

      } catch (SQLException e) {
        throw new UncheckedSQLException(e);
      }
    }

    private AutoCloseable createAutoClosable(PreparedStatement statement) {
      return () -> {
        if (autoClose) {
          try (PreparedStatement s = statement; Connection c = connection) { }
        } else {
          statement.close();
        }
      };
    }

    private void applyParameters(PreparedStatement statement) throws SQLException {
      if (params.length > 0 && !namedParams.isEmpty()) {
        throw new UnsupportedOperationException(
              "Unsupported parameter configuration (uses of named and anon parameters)");
      }
      if (params.length > 0) {
        applyStandardParams(statement);
      }
      else if (sql.contains(":")) {
        applyNamedParams(statement);
      }
    }

    private void applyStandardParams(PreparedStatement statement) throws SQLException {
      if (sql.chars().filter(c -> c == '?').count() != params.length) {
        throw new SQLException(
              "Parameters supplied do not correspond to SQL statement: " +
                    sql + ' ' + Arrays.toString(params));
      }

      for (int i = 0; i < params.length; i++) {
        statement.setObject(i+1, params[i]);
      }
    }

    private void applyNamedParams(PreparedStatement statement) throws SQLException {
      List<String> names = StringParameters.parameters(sql);
      for (int i = 0; i < names.size(); i++) {
        String name = names.get(i);
        statement.setObject(i+1, value(name));
      }
    }

    private Object value(String name) {
      Object value = namedParams.get(name);
      if (value == null) {
        throw new IllegalStateException("Unspecified parameter: " + name);
      }
      return value;
    }
  }

}

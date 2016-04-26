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
import java.util.stream.Stream;

/**
 * Database DSL.
 */
public class DB {

  public static DBConnection with(Connection connection) {
    return new DBConnection(() -> connection, false);
  }

  public static DBConnection with(DataSource dataSource) {
    return new DBConnection(dataSource::getConnection, true);
  }

  public static class DBConnection {
    private final ConnectionFactory connectionFactory;
    private final boolean autoClose;

    DBConnection(ConnectionFactory connectionFactory, boolean autoClose) {
      this.connectionFactory = connectionFactory;
      this.autoClose = autoClose;
    }

    public DBQueryBuilder query(String sql) {
      return new DBQueryBuilder(connectionFactory, autoClose, sql);
    }
  }

  public static class DBQueryBuilder {
    private final ConnectionFactory connectionFactory;
    private final boolean closeConnection;
    private final String sql;
    private final Map<String, Object> namedParams;
    private Object[] params;

    DBQueryBuilder(ConnectionFactory connectionFactory, boolean closeConnection, String sql) {
      this.connectionFactory = connectionFactory;
      this.closeConnection = closeConnection;
      this.sql = sql;
      this.namedParams = Maps.newHashMap();
      this.params = new Object[0];
    }

    public DBQueryBuilder params(Object... params) {
      this.params = params;
      return this;
    }

    public DBQueryBuilder param(String name, Object value) {
      namedParams.put(name, value);
      return this;
    }

    public <T> DBQuery<T> map(RowMapper<T> mapper) {
      ParameterisedQuery query = new ParameterisedQuery(sql, params, namedParams);
      return new DBQuery<>(connectionFactory, closeConnection, mapper, query);
    }
  }

  public static class DBQuery<T> {
    private final ConnectionFactory connectionFactory;
    private final boolean closeConnection;
    private final RowMapper<T> mapper;
    private final ParameterisedQuery query;

    DBQuery(ConnectionFactory connectionFactory, boolean closeConnection, RowMapper<T> mapper, ParameterisedQuery query) {
      this.connectionFactory = connectionFactory;
      this.closeConnection = closeConnection;
      this.mapper = mapper;
      this.query = query;
    }

    public Stream<T> stream() {
      try {

        Connection connection = connectionFactory.create();
        PreparedStatement statement = query.createStatement(connection);
        ResultSet resultSet = statement.executeQuery();
        return new ResultSetIterator<>(resultSet, mapper)
              .onClose(asSQLResource(statement, connection))
              .stream();

      } catch (SQLException e) {
        throw new UncheckedSQLException(e);
      }
    }

    private SQLResource asSQLResource(PreparedStatement statement, Connection connection) {
      return () -> {
        if (closeConnection) {
          try (PreparedStatement s = statement; Connection c = connection) { }
        } else {
          statement.close();
        }
      };
    }
  }

  private static class ParameterisedQuery {
    private final String sql;
    private final Object[] params;
    private final Map<String, Object> namedParams;

    ParameterisedQuery(String sql, Object[] params, Map<String, Object> namedParams) {
      this.sql = sql;
      this.params = params;
      this.namedParams = namedParams;
    }

    PreparedStatement createStatement(Connection connection) throws SQLException {
      String normalisedSQL = StringParameters.normalise(sql);
      PreparedStatement statement = connection.prepareStatement(normalisedSQL);
      applyParameters(statement);
      return statement;
    }

    void applyParameters(PreparedStatement statement) throws SQLException {
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

  private interface ConnectionFactory {
    Connection create() throws SQLException;
  }

}

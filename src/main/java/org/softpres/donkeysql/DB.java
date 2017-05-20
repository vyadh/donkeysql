/*
 * Copyright (c) 2015, Kieron Wilkinson
 */
package org.softpres.donkeysql;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Database DSL entry point.
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
      this.namedParams = new HashMap<>();
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

    public Stream<T> execute() {
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

  private interface ConnectionFactory {
    Connection create() throws SQLException;
  }

}

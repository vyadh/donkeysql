/*
 * Copyright (c) 2015, Kieron Wilkinson
 */
package org.softpres.donkeysql;

import org.softpres.donkeysql.params.ParamQuery;

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

    DBQueryBuilder(ConnectionFactory connectionFactory, boolean closeConnection, String sql) {
      this.connectionFactory = connectionFactory;
      this.closeConnection = closeConnection;
      this.sql = sql;
    }

    public IndexedQueryBuilder params(Object... params) {
      return new IndexedQueryBuilder(this, params);
    }

    public NamedQueryBuilder param(String name, Object value) {
      return new NamedQueryBuilder(this).param(name, value);
    }

    public <T> DBQuery<T> map(RowMapper<T> mapper) {
      return new DBQuery<>(
            connectionFactory,
            closeConnection,
            mapper,
            ParamQuery.none(sql));
    }
  }

  public static class IndexedQueryBuilder {
    private final DBQueryBuilder builder;
    private final Object[] params;

    IndexedQueryBuilder(DBQueryBuilder builder, Object[] params) {
      this.builder = builder;
      this.params = params;
    }

    public <T> DBQuery<T> map(RowMapper<T> mapper) {
      return new DBQuery<>(
            builder.connectionFactory,
            builder.closeConnection,
            mapper,
            ParamQuery.indexed(builder.sql, params));
    }
  }

  public static class NamedQueryBuilder {
    private final DBQueryBuilder builder;
    private final Map<String, Object> params;

    NamedQueryBuilder(DBQueryBuilder builder) {
      this.builder = builder;
      params = new HashMap<>();
    }

    public NamedQueryBuilder param(String name, Object value) {
      params.put(name, value);
      return this;
    }

    public <T> DBQuery<T> map(RowMapper<T> mapper) {
      return new DBQuery<>(
            builder.connectionFactory,
            builder.closeConnection,
            mapper,
            ParamQuery.named(builder.sql, params));
    }
  }

  public static class DBQuery<T> {
    private final ConnectionFactory connectionFactory;
    private final boolean closeConnection;
    private final RowMapper<T> mapper;
    private final ParamQuery query;

    DBQuery(ConnectionFactory connectionFactory, boolean closeConnection, RowMapper<T> mapper, ParamQuery query) {
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

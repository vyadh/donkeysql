/*
 * Copyright (c) 2015, Kieron Wilkinson
 */
package org.softpres.donkeysql;

import org.softpres.donkeysql.params.ParamQuery;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

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
      return new DBQueryBuilder(this, sql);
    }
  }

  public static class DBQueryBuilder {
    private final DBConnection connection;
    private final String sql;

    DBQueryBuilder(DBConnection connection, String sql) {
      this.connection = connection;
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
            connection.connectionFactory,
            connection.autoClose,
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
            builder.connection.connectionFactory,
            builder.connection.autoClose,
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
            builder.connection.connectionFactory,
            builder.connection.autoClose,
            mapper,
            ParamQuery.named(builder.sql, params));
    }
  }

}

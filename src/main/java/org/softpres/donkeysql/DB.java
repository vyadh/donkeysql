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

  public static ConnectionBuilder with(Connection connection) {
    return new ConnectionBuilder(() -> connection, false);
  }

  public static ConnectionBuilder with(DataSource dataSource) {
    return new ConnectionBuilder(dataSource::getConnection, true);
  }

  public static class ConnectionBuilder {
    private final ConnectionFactory connectionFactory;
    private final boolean autoClose;

    ConnectionBuilder(ConnectionFactory connectionFactory, boolean autoClose) {
      this.connectionFactory = connectionFactory;
      this.autoClose = autoClose;
    }

    public QueryBuilder query(String sql) {
      return new QueryBuilder(this, sql);
    }
  }

  /**
   * Signifies a class can construct a {@link StagedQuery} from a {@link RowMapper},
   * mainly used to allow code reuse at a call-site.
   */
  public interface MappableQuery {
    <T> StagedQuery<T> map(RowMapper<T> mapper);
  }

  public static class QueryBuilder implements MappableQuery {
    private final ConnectionBuilder connection;
    private final String sql;

    QueryBuilder(ConnectionBuilder connection, String sql) {
      this.connection = connection;
      this.sql = sql;
    }

    /** Convenience method to allow delegating setting named params. */
    public NamedQueryBuilder named() {
      return new NamedQueryBuilder(this);
    }

    public IndexedQueryBuilder params(Object... params) {
      return new IndexedQueryBuilder(this, params);
    }

    public NamedQueryBuilder param(String name, Object value) {
      return named().param(name, value);
    }

    public <T> StagedQuery<T> map(RowMapper<T> mapper) {
      return new StagedQuery<>(
            connection.connectionFactory,
            connection.autoClose,
            mapper,
            ParamQuery.none(sql));
    }
  }

  public static class IndexedQueryBuilder implements MappableQuery {
    private final QueryBuilder builder;
    private final Object[] params;

    IndexedQueryBuilder(QueryBuilder builder, Object[] params) {
      this.builder = builder;
      this.params = params;
    }

    public <T> StagedQuery<T> map(RowMapper<T> mapper) {
      return new StagedQuery<>(
            builder.connection.connectionFactory,
            builder.connection.autoClose,
            mapper,
            ParamQuery.indexed(builder.sql, params));
    }
  }

  public static class NamedQueryBuilder implements MappableQuery {
    private final QueryBuilder builder;
    private final Map<String, Object> params;

    NamedQueryBuilder(QueryBuilder builder) {
      this.builder = builder;
      params = new HashMap<>();
    }

    public NamedQueryBuilder param(String name, Object value) {
      params.put(name, value);
      return this;
    }

    public <T> StagedQuery<T> map(RowMapper<T> mapper) {
      return new StagedQuery<>(
            builder.connection.connectionFactory,
            builder.connection.autoClose,
            mapper,
            ParamQuery.named(builder.sql, params));
    }
  }

}

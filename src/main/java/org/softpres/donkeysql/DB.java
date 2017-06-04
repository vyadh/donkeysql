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

  /**
   * Start a DB query with an explicit {@link Connection}, which assumes any connection
   * management is being done at the call site. The connection will not be automatically
   * closed.
   */
  public static ConnectionBuilder with(Connection connection) {
    return new ConnectionBuilder(() -> connection, false);
  }

  /**
   * Start a DB query with the specified DataSource, which assumes any connection management
   * is either being done by an underlying pool, or that it is okay to close the connection
   * when done. The connection will be automatically closed when all the results have been
   * consumed.
   */
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

    /** Convenience method to allow delegating setting of named params. */
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

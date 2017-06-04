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

  private final ConnectionFactory connectionFactory;
  private final boolean autoCloseConnection;

  private DB(ConnectionFactory connectionFactory, boolean autoCloseConnection) {
    this.connectionFactory = connectionFactory;
    this.autoCloseConnection = autoCloseConnection;
  }

  /**
   * Start a DB query with an explicit {@link Connection}, which assumes any connection
   * management is being done at the call site. The connection will not be automatically
   * closed.
   */
  public static DB with(Connection connection) {
    return new DB(() -> connection, false);
  }

  /**
   * Start a DB query with the specified DataSource, which assumes any connection management
   * is either being done by an underlying pool, or that it is okay to close the connection
   * when done. The connection will be automatically closed when all the results have been
   * consumed.
   */
  public static DB with(DataSource dataSource) {
    return new DB(dataSource::getConnection, true);
  }

  /**
   * A query is usually constructed from a static SQL statement, the returned builder here
   * allowing the specification of any required parameters before mapping the result for
   * execution.
   */
  public QueryBuilder query(String sql) {
    return new QueryBuilder(sql);
  }

  /**
   * Signifies a class can construct a {@link StagedQuery} from a {@link RowMapper},
   * mainly used to allow code reuse at a call-site.
   */
  public interface MappableQuery {
    <T> StagedQuery<T> map(RowMapper<T> mapper);
  }

  public class QueryBuilder implements MappableQuery {
    private final String sql;

    QueryBuilder(String sql) {
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
            connectionFactory,
            autoCloseConnection,
            mapper,
            ParamQuery.none(sql));
    }
  }

  public class IndexedQueryBuilder implements MappableQuery {
    private final QueryBuilder builder;
    private final Object[] params;

    IndexedQueryBuilder(QueryBuilder builder, Object[] params) {
      this.builder = builder;
      this.params = params;
    }

    public <T> StagedQuery<T> map(RowMapper<T> mapper) {
      return new StagedQuery<>(
            connectionFactory,
            autoCloseConnection,
            mapper,
            ParamQuery.indexed(builder.sql, params));
    }
  }

  public class NamedQueryBuilder implements MappableQuery {
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
            connectionFactory,
            autoCloseConnection,
            mapper,
            ParamQuery.named(builder.sql, params));
    }
  }

}

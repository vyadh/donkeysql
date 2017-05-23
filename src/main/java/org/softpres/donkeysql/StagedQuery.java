/*
 * Copyright (c) 2017, Kieron Wilkinson
 */
package org.softpres.donkeysql;

import org.softpres.donkeysql.params.ParamQuery;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Holder for all the information that a query needs to be performed.
 */
public class StagedQuery<T> {

  private final ConnectionFactory connectionFactory;
  private final boolean closeConnection;
  private final RowMapper<T> mapper;
  private final ParamQuery query;

  StagedQuery(ConnectionFactory connectionFactory, boolean closeConnection, RowMapper<T> mapper, ParamQuery query) {
    this.connectionFactory = connectionFactory;
    this.closeConnection = closeConnection;
    this.mapper = mapper;
    this.query = query;
  }

  /**
   * Fluent mechanism for peeking at the resulting (logical) SQL statement,
   * where any parameters are replaced with their respective values.
   */
  public StagedQuery<T> peek(Consumer<String> sql) {
    sql.accept(query.toString());
    return this;
  }

  /**
   * Execute the query by creating the required connection, executing the prepared statement,
   * and performing the mapping of results using the previously supplied mapping function.
   */
  public Stream<T> execute() {
    try {
      return executeThrowing();
    } catch (SQLException e) {
      throw new UncheckedSQLException(e);
    }
  }

  private Stream<T> executeThrowing() throws SQLException {
    Connection connection = connectionFactory.create();
    PreparedStatement statement = query.createStatement(connection);
    ResultSet resultSet = statement.executeQuery();
    return new ResultSetIterator<>(resultSet, mapper)
          .onClose(asSQLResource(statement, connection))
          .stream();
  }

  private QueryResource asSQLResource(PreparedStatement statement, Connection connection) {
    return () -> {
      if (closeConnection) {
        try (PreparedStatement s = statement; Connection c = connection) {
          // Used to close resources
        }
      } else {
        statement.close();
      }
    };
  }

}

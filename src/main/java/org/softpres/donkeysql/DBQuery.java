/*
 * Copyright (c) 2017, Kieron Wilkinson
 */
package org.softpres.donkeysql;

import org.softpres.donkeysql.params.ParamQuery;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.stream.Stream;

/**
 * Main query execution loop.
 */
public class DBQuery<T> {

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
        try (PreparedStatement s = statement; Connection c = connection) {
        }
      } else {
        statement.close();
      }
    };
  }

}

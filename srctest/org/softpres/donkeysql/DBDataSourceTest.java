/*
 * Copyright (c) 2016, Kieron Wilkinson
 */
package org.softpres.donkeysql;

import org.junit.Test;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests for closing a connection when passed a DataSource.
 */
public class DBDataSourceTest {

  @Test
  public void exceptionOnConnectionCreationThrowsUncheckedSQLException() throws SQLException {
    SQLException exception = new SQLException("server down");
    DataSource dataSource = mock(DataSource.class);
    when(dataSource.getConnection()).thenThrow(exception);

    Throwable throwable = catchThrowable(() ->
      DB.with(dataSource)
            .query("")
            .map(rs -> rs)
            .stream()
    );

    assertThat(throwable)
          .isInstanceOf(UncheckedSQLException.class)
          .hasMessage(exception.getMessage())
          .hasCause(exception);
  }

  @Test
  public void resourcesClosedWhenUsingConnection() throws SQLException {
    ResultSet resultSet = singleResult(42);
    PreparedStatement statement = statement(resultSet);
    Connection connection = connection(statement);

    DB.with(connection)
          .query("SELECT 42")
          .map(rs -> rs.getInt(1))
          .stream()
          .collect(toList());

    verify(statement).close();
    verify(connection, never()).close();
  }

  @Test
  public void resourcesClosedWhenUsingDataSource() throws SQLException {
    ResultSet resultSet = singleResult(42);
    PreparedStatement statement = statement(resultSet);
    Connection connection = connection(statement);
    DataSource dataSource = dataSource(connection);

    DB.with(dataSource)
          .query("SELECT 42")
          .map(rs -> rs.getInt(1))
          .stream()
          .collect(toList());

    verify(statement).close();
    verify(connection).close();
  }

  @Test
  public void statementCloseThrowingExceptionStillClosesConnection() throws SQLException {
    ResultSet resultSet = singleResult(42);
    PreparedStatement statement = statement(resultSet);
    SQLException statementCloseException = new SQLException("statement close");
    doThrow(statementCloseException).when(statement).close();
    Connection connection = connection(statement);

    Throwable throwable = catchThrowable(() -> DB.with(dataSource(connection))
          .query("SELECT 42")
          .map(rs -> rs.getInt(1))
          .stream()
          .collect(toList()));

    assertThat(throwable)
          .isInstanceOf(UncheckedSQLException.class)
          .hasCause(statementCloseException);
    verify(statement, atLeastOnce()).close();
    verify(connection, atLeastOnce()).close();
  }

  private ResultSet singleResult(int value) throws SQLException {
    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.next()).thenReturn(true, false);
    when(resultSet.getInt(1)).thenReturn(value);
    return resultSet;
  }

  private PreparedStatement statement(ResultSet resultSet) throws SQLException {
    PreparedStatement statement = mock(PreparedStatement.class);
    when(statement.executeQuery()).thenReturn(resultSet);
    return statement;
  }

  private Connection connection(PreparedStatement statement) throws SQLException {
    Connection connection = mock(Connection.class);
    when(connection.prepareStatement(anyString())).thenReturn(statement);
    return connection;
  }

  private DataSource dataSource(Connection connection) throws SQLException {
    DataSource dataSource = mock(DataSource.class);
    when(dataSource.getConnection()).thenReturn(connection);
    return dataSource;
  }

}

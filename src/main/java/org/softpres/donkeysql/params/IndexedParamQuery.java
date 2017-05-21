/*
 * Copyright (c) 2017, Kieron Wilkinson
 */
package org.softpres.donkeysql.params;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static java.util.stream.Collectors.joining;

/**
 * Associates an indexed parameterised SQL statement with the values to populate it.
 */
class IndexedParamQuery implements ParamQuery {

  private final String sql;
  private final Object[] params;

  IndexedParamQuery(String sql, Object[] params) {
    this.sql = sql;
    this.params = params;
  }

  @Override
  public PreparedStatement createStatement(Connection connection) throws SQLException {
    PreparedStatement statement = connection.prepareStatement(sql);
    applyParameters(statement);
    return statement;
  }

  private void applyParameters(PreparedStatement statement) throws SQLException {
    check();

    for (int i = 0; i < params.length; i++) {
      statement.setObject(i + 1, params[i]);
    }
  }

  private void check() {
    if (count(sql) != params.length) {
      throw new MismatchedParametersException(sql, params);
    }
  }

  /**
   * Count the number of indexed parameters in a normalised statement, not including any
   * '?' characters existing within quoted values.
   */
  static long count(String statement) {
    return StringParameters.tokenise(statement).stream()
          .filter(token -> token instanceof StringParameters.IndexedParam)
          .count();
  }

  @Override
  public String toString() {
    return humanise(sql, params);
  }

  /**
   * Replace all the parameters in an SQL statement with the supplied parameter values.
   * The result should be an SQL statement as a human would want to read it.
   *
   * @param statement an SQL statement, which should have been normalised (no named params).
   */
  static String humanise(String statement, Object... params) {
    if (count(statement) != params.length) {
      throw new MismatchedParametersException(statement, params);
    }

    ParamIterator paramIterator = new ParamIterator(params);

    return StringParameters.tokenise(statement).stream()
          .map(token -> token instanceof StringParameters.IndexedParam ?
                Humanise.paramValue(paramIterator.next()) : token.text)
          .collect(joining());
  }

  /** Allows iteration of parameters. Assumes count has already been validated. */
  private static class ParamIterator {
    private int index;
    private final Object[] params;

    ParamIterator(Object[] params) {
      index = 0;
      this.params = params;
    }

    Object next() {
      return params[index++];
    }
  }

}

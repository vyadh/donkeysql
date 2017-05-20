/*
 * Copyright (c) 2017, Kieron Wilkinson
 */
package org.softpres.donkeysql.params;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;

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
    if (sql.chars().filter(c -> c == '?').count() != params.length) {
      throw new SQLException(
            "Parameters supplied do not correspond to SQL statement: " +
                  sql + ' ' + Arrays.toString(params));
    }

    for (int i = 0; i < params.length; i++) {
      statement.setObject(i + 1, params[i]);
    }
  }

}

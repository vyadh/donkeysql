/*
 * Copyright (c) 2017, Kieron Wilkinson
 */
package org.softpres.donkeysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Associates a parameterised SQL statement with the values to populate it.
 * Supports named parameters as well as standard '?' placeholders.
 */
class ParameterisedQuery {

  private final String sql;
  private final Object[] params;
  private final Map<String, Object> namedParams;

  ParameterisedQuery(String sql, Object[] params, Map<String, Object> namedParams) {
    this.sql = sql;
    this.params = params;
    this.namedParams = namedParams;
  }

  PreparedStatement createStatement(Connection connection) throws SQLException {
    String normalisedSQL = StringParameters.normalise(sql);
    PreparedStatement statement = connection.prepareStatement(normalisedSQL);
    applyParameters(statement);
    return statement;
  }

  private void applyParameters(PreparedStatement statement) throws SQLException {
    if (params.length > 0 && !namedParams.isEmpty()) {
      throw new UnsupportedOperationException(
            "Unsupported parameter configuration (uses of named and anon parameters)");
    }
    if (params.length > 0) {
      applyStandardParams(statement);
    } else if (sql.contains(":")) {
      applyNamedParams(statement);
    }
  }

  private void applyStandardParams(PreparedStatement statement) throws SQLException {
    if (sql.chars().filter(c -> c == '?').count() != params.length) {
      throw new SQLException(
            "Parameters supplied do not correspond to SQL statement: " +
                  sql + ' ' + Arrays.toString(params));
    }

    for (int i = 0; i < params.length; i++) {
      statement.setObject(i + 1, params[i]);
    }
  }

  private void applyNamedParams(PreparedStatement statement) throws SQLException {
    List<String> names = StringParameters.parameters(sql);
    for (int i = 0; i < names.size(); i++) {
      String name = names.get(i);
      statement.setObject(i + 1, value(name));
    }
  }

  private Object value(String name) {
    Object value = namedParams.get(name);
    if (value == null) {
      throw new IllegalStateException("Unspecified parameter: " + name);
    }
    return value;
  }

}

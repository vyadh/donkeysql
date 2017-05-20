/*
 * Copyright (c) 2017, Kieron Wilkinson
 */
package org.softpres.donkeysql.params;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Associates a "named" parameterised SQL statement with the values to populate it.
 */
class NamedParamQuery implements ParamQuery {

  private final String sql;
  private final Map<String, Object> params;

  NamedParamQuery(String sql, Map<String, Object> params) {
    this.sql = sql;
    this.params = params;
  }

  @Override
  public PreparedStatement createStatement(Connection connection) throws SQLException {
    String normalisedSQL = StringParameters.normalise(sql);
    PreparedStatement statement = connection.prepareStatement(normalisedSQL);
    applyParameters(statement);
    return statement;
  }

  private void applyParameters(PreparedStatement statement) throws SQLException {
    List<String> names = StringParameters.parameters(sql);
    for (int i = 0; i < names.size(); i++) {
      String name = names.get(i);
      statement.setObject(i + 1, value(name));
    }
  }

  private Object value(String name) {
    Object value = params.get(name);
    if (value == null) {
      throw new IllegalStateException("Unspecified parameter: " + name);
    }
    return value;
  }

}

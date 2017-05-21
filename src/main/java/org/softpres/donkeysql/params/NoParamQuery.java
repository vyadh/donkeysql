/*
 * Copyright (c) 2017, Kieron Wilkinson
 */
package org.softpres.donkeysql.params;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Query where no parameters have been supplied.
 */
class NoParamQuery implements ParamQuery {

  private final String sql;

  NoParamQuery(String sql) {
    this.sql = sql;
  }

  @Override
  public PreparedStatement createStatement(Connection connection) throws SQLException {
    return connection.prepareStatement(sql);
  }

  @Override
  public String toString() {
    return sql;
  }

}

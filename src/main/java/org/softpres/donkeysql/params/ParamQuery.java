/*
 * Copyright (c) 2017, Kieron Wilkinson
 */
package org.softpres.donkeysql.params;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

/**
 * Covers different types of query-param associations (named and indexed).
 */
public interface ParamQuery {

  PreparedStatement createStatement(Connection connection) throws SQLException;

  static ParamQuery indexed(String sql, Object[] params) {
    return new IndexedParamQuery(sql, params);
  }

  static ParamQuery named(String sql, Map<String, Object> params) {
    return new NamedParamQuery(sql, params);
  }

  static ParamQuery none(String sql) {
    return new NoParamQuery(sql);
  }

}

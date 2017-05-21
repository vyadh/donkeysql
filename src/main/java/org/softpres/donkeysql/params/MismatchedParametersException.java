/*
 * Copyright (c) 2017, Kieron Wilkinson
 */
package org.softpres.donkeysql.params;

import org.softpres.donkeysql.UncheckedSQLException;

import java.util.Arrays;

/**
 * Exception to highlight mismatches between specified parameters in a statement,
 * and those actually supplied.
 */
public class MismatchedParametersException extends UncheckedSQLException {

  public MismatchedParametersException(String statement, Object[] params) {
    super(message(statement, params));
  }

  private static String message(String sql, Object[] params) {
    return "Parameters supplied do not correspond to SQL statement: " +
          sql + ' ' + Arrays.toString(params);
  }

}

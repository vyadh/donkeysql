/*
 * Copyright (c) 2015, Kieron Wilkinson
 */
package org.softpres.donkeysql;

import java.sql.SQLException;

/**
 * Checked exceptions cannot be used through interfaces such as {@link java.util.Iterator}
 * and {@link java.util.stream.Stream}, so we use an unchecked one to avoid compromising
 * our API goals.
 */
public class UncheckedSQLException extends RuntimeException {

  public UncheckedSQLException(String message) {
    super(message);
  }

  public UncheckedSQLException(SQLException e) {
    super(e.getMessage(), e);
  }

}

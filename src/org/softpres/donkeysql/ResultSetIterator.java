/*
 * Copyright (c) $today.year, Kieron Wilkinson.
 */
package org.softpres.donkeysql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.softpres.donkeysql.ResultSetIterator.Next.*;

/**
 * Iterator over the results of a ResultSetQuery.
 * This class is not thread safe.
 */
public class ResultSetIterator implements Iterator<Integer>, AutoCloseable {

  private final ResultSet resultSet;
  private Next next;

  public ResultSetIterator(ResultSet resultSet) {
    this.resultSet = resultSet;
    next = UNKNOWN;
  }

  @Override
  public boolean hasNext() {
    try {
      updateNext();
      return next == FOUND;
    } catch (SQLException e) {
      closeQuietly();
      throw new UncheckedSQLException(e);
    }
  }

  private void updateNext() throws SQLException {
    if (next == UNKNOWN) {
      next = resultSet.next() ? FOUND : FINISHED;
    }
    if (next == FINISHED) {
      close();
    }
  }

  @Override
  public Integer next() {
    try {
      if (hasNext()) {
        int result = resultSet.getInt(1);
        next = UNKNOWN;
        return result;
      }
    } catch (SQLException e) {
      closeQuietly();
      throw new UncheckedSQLException(e);
    }
    throw new NoSuchElementException();
  }

  private void closeQuietly() {
    try {
      close();
    } catch (Exception ignore) {
    }
  }

  @Override
  public void close() throws SQLException {
    resultSet.close();
  }

  enum Next {
    UNKNOWN,
    FOUND,
    FINISHED
  }

}

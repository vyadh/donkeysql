/*
 * Copyright (c) $today.year, Kieron Wilkinson.
 */
package org.softpres.donkeysql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.softpres.donkeysql.ResultSetIterator.Next.*;

/**
 * Iterator over the results of a ResultSetQuery.
 * This class is not thread safe.
 */
public class ResultSetIterator<T> implements Iterator<T>, AutoCloseable {

  private final ResultSet resultSet;
  private final RowMapper<T> mapper;
  private Next next;
  private QueryResource onClose;

  public ResultSetIterator(ResultSet resultSet, RowMapper<T> mapper) {
    this.resultSet = resultSet;
    this.mapper = mapper;
    next = UNKNOWN;
    onClose = () -> {};
  }

  ResultSetIterator<T> onClose(QueryResource onClose) {
    this.onClose = onClose;
    return this;
  }

  @Override
  public boolean hasNext() {
    try {
      updateNext();
      return next == FOUND;
    } catch (SQLException e) {
      closeQuietly();
      throw new UncheckedSQLException(e);
    } catch (Exception e) {
      closeQuietly();
      throw e;
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
  public T next() {
    try {
      if (hasNext()) {
        return mapper.apply(resultSet);
      }
    } catch (SQLException e) {
      closeQuietly();
      throw new UncheckedSQLException(e);
    } catch (Exception e) {
      closeQuietly();
      throw e;
    } finally {
      next = UNKNOWN;
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
    next = FINISHED;

    try (ResultSet rs = resultSet; QueryResource other = onClose) { }
  }

  enum Next {
    UNKNOWN,
    FOUND,
    FINISHED
  }

  public Stream<T> stream() {
    Spliterator<T> spliterator = Spliterators.spliteratorUnknownSize(this, 0);
    Stream<T> stream = StreamSupport.stream(spliterator, false);
    return stream.onClose(this::closeQuietly);
  }

  /** A last-ditch effort to clean up in case it wasn't another way. */
  @Override
  protected void finalize() throws Throwable {
    super.finalize();

    if (next != FINISHED) {
      closeQuietly();
    }
  }

}

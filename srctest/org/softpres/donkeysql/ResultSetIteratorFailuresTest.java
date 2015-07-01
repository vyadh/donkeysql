/*
 * Copyright (c) $today.year, Kieron Wilkinson.
 */
package org.softpres.donkeysql;

import org.junit.Before;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.NoSuchElementException;

import static org.assertj.core.api.StrictAssertions.assertThat;
import static org.assertj.core.api.StrictAssertions.catchThrowable;
import static org.mockito.Mockito.*;

/**
 * Unit test for connection logic of {@link ResultSetIterator}.
 */
@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
public class ResultSetIteratorFailuresTest {

  private ResultSetIterator<Integer> iterator;
  private ResultSet resultSet;

  @Before
  public void setup() throws SQLException {
    resultSet = mock(ResultSet.class);
    iterator = new ResultSetIterator<>(resultSet, rs -> rs.getInt(1));
  }

  @Test
  public void failureToIterateResultSetAttemptsToCloseIt() throws SQLException {
    when(resultSet.next()).thenThrow(new SQLException("error"));

    catchThrowable(iterator::next);

    verify(resultSet, atLeastOnce()).close();
  }

  @Test
  public void failureToGetInformationFromResultSetClosesIt() throws SQLException {
    when(resultSet.next()).thenReturn(true);
    when(resultSet.getInt(anyInt())).thenThrow(new SQLException("error"));

    catchThrowable(iterator::next);

    verify(resultSet).close();
  }

  @Test
  public void failureToConvertWithExceptionClosesResultSet() throws SQLException {
    when(resultSet.next()).thenReturn(true);

    ResultSetIterator<Integer> iterator = new ResultSetIterator<>(resultSet, rs -> {
      throw new IllegalStateException("error");
    });

    Throwable throwable = catchThrowable(iterator::next);

    verify(resultSet, atLeastOnce()).close();
    assertThat(throwable)
          .isInstanceOf(IllegalStateException.class)
          .hasMessage("error");
  }

  @Test
  @SuppressWarnings("EmptyTryBlock")
  public void autoClosableContractRespected() throws SQLException {
    try (ResultSetIterator<Integer> rsi = iterator) {
    }

    verify(resultSet).close();
  }

  @Test
  public void callingNextWhenNoMoreItemsThrowsException() throws SQLException {
    when(resultSet.next()).thenReturn(false);

    Throwable throwable = catchThrowable(iterator::next);

    assertThat(throwable).isInstanceOf(NoSuchElementException.class);
  }

}

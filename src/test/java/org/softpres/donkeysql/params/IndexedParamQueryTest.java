/*
 * Copyright (c) 2017, Kieron Wilkinson
 */
package org.softpres.donkeysql.params;

import org.junit.Test;
import org.softpres.donkeysql.UncheckedSQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.softpres.donkeysql.params.IndexedParamQuery.count;
import static org.softpres.donkeysql.params.IndexedParamQuery.humanise;

/**
 * Unit tests for {@link IndexedParamQuery}.
 */
public class IndexedParamQueryTest {

  private final Object[] noParams = { };

  @Test
  public void countNormalisedParameters() {
    assertThat(count("SELECT * FROM t WHERE a = 'id'")).isZero();
    assertThat(count("SELECT * FROM t WHERE a = ?")).isEqualTo(1);
    assertThat(count("SELECT * FROM t WHERE a = ? AND b = ?")).isEqualTo(2);
    assertThat(count("SELECT * FROM t WHERE a = ? AND b = '?' AND c = ?")).isEqualTo(2);
  }

  @Test
  public void humaniseWithNoParameters() {
    assertThat(
          humanise("SELECT * FROM table WHERE column = 'id'", noParams))
          .isEqualTo("SELECT * FROM table WHERE column = 'id'");
  }

  @Test
  public void humaniseWithNumericParameters() {
    assertThat(
          humanise("SELECT * FROM table WHERE id = ? AND size > ?", 1, 100))
          .isEqualTo("SELECT * FROM table WHERE id = 1 AND size > 100");
  }

  @Test
  public void humaniseWithNonNumericParameters() {
    assertThat(
          humanise("SELECT * FROM table WHERE column = ? AND type > ?", "name", "varchar"))
          .isEqualTo("SELECT * FROM table WHERE column = 'name' AND type > 'varchar'");
  }

  @Test
  public void humaniseWithQuotedMarksNotUsedAsParameter() {
    assertThat(
          humanise("WHERE a > ? AND b = '?' AND c = ?", 1, "2"))
          .isEqualTo("WHERE a > 1 AND b = '?' AND c = '2'");
  }

  @Test
  public void humaniseWithTooFewParameters() {
    Throwable error = catchThrowable(() ->
          humanise("SELECT * FROM table WHERE column = ? AND size = ?", 1));

    assertThat(error)
          .isInstanceOf(UncheckedSQLException.class)
          .hasMessageStartingWith("Parameters supplied do not correspond to SQL statement");
  }

  @Test
  public void humaniseWithTooManyParameters() {
    Throwable error = catchThrowable(() ->
          humanise("SELECT * FROM table WHERE column = ?", 1, 2));

    assertThat(error)
          .isInstanceOf(UncheckedSQLException.class)
          .hasMessageStartingWith("Parameters supplied do not correspond to SQL statement");
  }

}

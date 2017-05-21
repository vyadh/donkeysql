/*
 * Copyright (c) 2017, Kieron Wilkinson
 */
package org.softpres.donkeysql.tokeniser;

import org.junit.Test;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link StatementTokeniser}.
 */
public class StringTokeniserTest {

  @Test
  public void parametersWithEmptyString() {
    assertThat(parameters(""))
          .isEmpty();
  }

  @Test
  public void parametersWithNoParameters() {
    assertThat(parameters("SELECT * FROM table WHERE id > 100"))
          .isEmpty();
  }

  @Test
  public void parametersWithOneParameter() {
    assertThat(parameters("stuff WHERE something < :id"))
          .containsExactly("id");
  }

  @Test
  public void parametersWithMultipleParameters() {
    assertThat(parameters("SELECT stuff FROM table WHERE something < :abc AND other = :xyz"))
          .containsExactly("abc", "xyz");
  }

  @Test
  public void parametersWithDuplicateParameters() {
    assertThat(parameters("SELECT count(1) FROM people WHERE :age >= 18 AND :age <= 60"))
          .containsExactly("age", "age");
  }

  @Test
  public void parametersWithRoundBracketsEitherSide() {
    assertThat(parameters("SELECT count(1) FROM people WHERE (:low >= 18 AND 60 > :high)"))
          .containsExactly("low", "high");
  }

  @Test
  public void parametersWithComplexStatement() {
    assertThat(parameters(
          "SELECT count(1) FROM people WHERE" +
                " (name LIKE :name_pattern) AND" +
                " ((:age >= 18 AND :age <= 60) OR (sibling LIKE :sister))"))
          .containsExactly("name_pattern", "age", "age", "sister");
  }

  /**
   * Returns the parameter names specified in the statement at the appropriate positions
   * in the list, which allows supporting duplicate names in the statement.
   */
  static Stream<String> parameters(String statement) {
    return StatementTokeniser.tokenise(statement).stream()
          .filter(token -> token instanceof StatementTokeniser.NamedParam)
          .map(token -> token.text);
  }

}

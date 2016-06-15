/*
 * Copyright (c) 2015, Kieron Wilkinson
 */

package org.softpres.donkeysql;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.softpres.donkeysql.StringParameters.normalise;
import static org.softpres.donkeysql.StringParameters.parameters;

/**
 * Unit tests for {@link StringParameters}.
 */
public class StringParametersTest {

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

  @Test
  public void normaliseWithNoParameters() {
    assertThat(
          normalise(""))
          .isEqualTo("");
    assertThat(
          normalise("something"))
          .isEqualTo("something");
    assertThat(
          normalise("SELECT * FROM people WHERE ages > ?"))
          .isEqualTo("SELECT * FROM people WHERE ages > ?");
  }

  @Test
  public void normaliseWithParameters() {
    assertThat(
          normalise("SELECT * FROM table WHERE column = :something"))
          .isEqualTo("SELECT * FROM table WHERE column = ?");
    assertThat(
          normalise("SELECT * FROM people WHERE ages < :too_young"))
          .isEqualTo("SELECT * FROM people WHERE ages < ?");
    assertThat(
          normalise("string :with many :params AND (:some with :brackets)"))
          .isEqualTo("string ? many ? AND (? with ?)");
  }

  @Test
  public void normaliseShouldNotReplaceParameterLikeValuesInLiteral() {
    String statement = "SELECT * FROM table WHERE column = 'prefix :something postfix'";

    assertThat(normalise(statement)).isEqualTo(statement);
  }

  @Test
  public void normaliseShouldNotReplaceParameterLikeValuesInTimestamp() {
    String statement = "SELECT * FROM people WHERE birth = '2000-01-01T00:00:00'";

    assertThat(normalise(statement)).isEqualTo(statement);
  }

  @Test
  public void normaliseShouldSupportOperators() {
    assertThat(normalise("WHERE name!=:first")).isEqualTo("WHERE name!=?");
    assertThat(normalise("WHERE name!='Kieron'")).isEqualTo("WHERE name!='Kieron'");
    assertThat(normalise("WHERE name LIKE 'Kieron %'")).isEqualTo("WHERE name LIKE 'Kieron %'");
    assertThat(normalise("WHERE age='18'")).isEqualTo("WHERE age='18'");
    assertThat(normalise("WHERE age>=:adult")).isEqualTo("WHERE age>=?");
  }

  @Test
  public void normaliseForParametersWithinAnInDeclarationWithCommas() {
    assertThat(normalise("SELECT * FROM people WHERE favouriteCol in (:color1, :colour2, :colour3)"))
          .isEqualTo("SELECT * FROM people WHERE favouriteCol in (?, ?, ?)");
  }

}

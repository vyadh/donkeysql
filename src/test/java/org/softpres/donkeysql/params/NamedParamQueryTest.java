/*
 * Copyright (c) 2017, Kieron Wilkinson
 */

package org.softpres.donkeysql.params;

import org.junit.Test;

import java.util.*;
import java.util.stream.Stream;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.softpres.donkeysql.params.NamedParamQuery.parameterValues;
import static org.softpres.donkeysql.params.NamedParamQuery.parameters;

/**
 * Unit tests for {@link NamedParamQuery}.
 */
public class NamedParamQueryTest {

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
  public void parameterValuesWhenMissing() {
    assertThat(parameterValues("SELECT count(1) FROM people WHERE age >= :adult", params()))
          .containsNull();
  }

  @Test
  public void parameterValuesWhenNumericStringOrIterable() {
    Stream<Object> values = parameterValues(
          "SELECT count(1) " +
                "FROM people " +
                "WHERE age >= :adult AND name LIKE :name AND county IN (:search)",
          params(
                "adult", 18,
                "name", "Bob%",
                "search", Arrays.asList("Kent", "Surrey")
          ));

    assertThat(values).containsExactly(18, "Bob%", "Kent", "Surrey");
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

  @Test
  public void normaliseWithZeroIterableItems() {
    assertThat(normalise("WHERE item IN (:items)", params("items", items(1))))
          .isEqualTo("WHERE item IN (?)");
  }

  @Test
  public void normaliseWithSingleIterableItem() {
    assertThat(normalise("WHERE item IN (:items)", params("items", items())))
          .isEqualTo("WHERE item IN ()");
  }

  @Test
  public void normaliseWithMultipleIterableItem() {
    assertThat(normalise("WHERE item IN (:items)", params("items", items(1, "2", 3))))
          .isEqualTo("WHERE item IN (?,?,?)");
  }


  private String normalise(String sql) {
    return new NamedParamQuery(
          sql,
          // Make params available, just used to detect iterable values
          NamedParamQuery.parameters(sql)
                .collect(toMap(identity(), identity()))
    ).normalise();
  }

  private String normalise(String sql, Map<String, Object> params) {
    return new NamedParamQuery(sql, params).normalise();
  }

  private Map<String, Object> params(Object... params) {
    Map<String, Object> result = new HashMap<>();
    for (int i = 0; i < params.length; i += 2) {
      result.put((String)params[i], params[i+1]);
    }
    return result;
  }

  private List<Object> items(Object... items) {
    return Arrays.asList(items);
  }

}

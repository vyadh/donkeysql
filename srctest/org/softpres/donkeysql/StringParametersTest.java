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
    assertThat(parameters("select * from table where id > 100"))
          .isEmpty();
  }

  @Test
  public void parametersWithOneParameter() {
    assertThat(parameters("stuff where something < :id"))
          .containsExactly("id");
  }

  @Test
  public void parametersWithMultipleParameters() {
    assertThat(parameters("select stuff from table where something < :abc and other = :xyz"))
          .containsExactly("abc", "xyz");
  }

  @Test
  public void parametersWithDuplicateParameters() {
    assertThat(parameters("select count(1) from people where :age >= 18 and :age <= 60"))
          .containsExactly("age", "age");
  }

  @Test
  public void parametersWithRoundBracketsEitherSide() {
    assertThat(parameters("select count(1) from people where (:low >= 18 and 60 > :high)"))
          .containsExactly("low", "high");
  }

  @Test
  public void parametersWithComplexStatement() {
    assertThat(parameters(
          "select count(1) from people where" +
                " (name like :name-pattern) and" +
                " ((:age >= 18 and :age <= 60) or (sibling like :sister))"))
          .containsExactly("name-pattern", "age", "age", "sister");
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
          normalise("select * from people where ages > ?"))
          .isEqualTo("select * from people where ages > ?");
  }

  @Test
  public void normaliseWithParameters() {
    assertThat(
          normalise("select * from table where column = :something"))
          .isEqualTo("select * from table where column = ?");
    assertThat(
          normalise("select * from people where ages < :too-young"))
          .isEqualTo("select * from people where ages < ?");
    assertThat(
          normalise("string :with many :params and (:some with :brackets)"))
          .isEqualTo("string ? many ? and (? with ?)");
  }

}

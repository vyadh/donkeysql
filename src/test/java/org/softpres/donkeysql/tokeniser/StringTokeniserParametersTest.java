/*
 * Copyright (c) 2017, Kieron Wilkinson
 */
package org.softpres.donkeysql.tokeniser;

import org.junit.Test;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.softpres.donkeysql.tokeniser.StatementTokeniser.*;

/**
 * Unit tests for {@link StatementTokeniser}.
 */
public class StringTokeniserParametersTest {

  @Test
  public void parametersWithEmptyString() {
    assertThat(tokenise(""))
          .isEmpty();
  }

  @Test
  public void statementWithLiteralValue() {
    assertThat(tokenise("SELECT * FROM table WHERE id > 100")).containsExactly(
          new Word("SELECT"),
          new Punc('*'),
          new Word("FROM"),
          new Word("table"),
          new Word("WHERE"),
          new Word("id"),
          new Punc('>'),
          new Word("100")
    );
  }

  @Test
  public void statementWithQuotedValue() {
    assertThat(tokenise("SELECT * FROM table WHERE column LIKE '%VAL'")).containsExactly(
          new Word("SELECT"),
          new Punc('*'),
          new Word("FROM"),
          new Word("table"),
          new Word("WHERE"),
          new Word("column"),
          new Word("LIKE"),
          new Quote(),
          new QuotedWord("%VAL"),
          new Quote()
    );
  }

  @Test
  public void parametersWithNamedParameters() {
    assertThat(tokenise("WHERE something < :abc AND other = :xyz")).containsExactly(
          new Word("WHERE"),
          new Word("something"),
          new Punc('<'),
          new NamedParam("abc"),
          new Word("AND"),
          new Word("other"),
          new Punc('='),
          new NamedParam("xyz")
    );
  }

  @Test
  public void parametersWithIndexedParameters() {
    assertThat(tokenise("WHERE something < ? AND other = ?")).containsExactly(
          new Word("WHERE"),
          new Word("something"),
          new Punc('<'),
          new IndexedParam(),
          new Word("AND"),
          new Word("other"),
          new Punc('='),
          new IndexedParam()
    );
  }

  @Test
  public void whereWithInCondition() {
    assertThat(tokenise("WHERE id IN (?, ?, ?)")).containsExactly(
          new Word("WHERE"),
          new Word("id"),
          new Word("IN"),
          new Punc('('),
          new IndexedParam(),
          new Punc(','),
          new IndexedParam(),
          new Punc(','),
          new IndexedParam(),
          new Punc(')')
    );
  }

  private static Stream<Token> tokenise(String statement) {
    return StatementTokeniser.tokenise(statement).stream()
          .filter(token -> !(token instanceof Space));
  }

}

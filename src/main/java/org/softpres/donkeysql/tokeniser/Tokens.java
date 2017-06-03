/*
 * Copyright (c) 2017, Kieron Wilkinson
 */
package org.softpres.donkeysql.tokeniser;

import java.util.Objects;

/**
 * Groups all the possible tokenised words in an SQL statement.
 */
public class Tokens {

  public abstract static class Token {
    public final String text;

    protected Token(String text) {
      this.text = text;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Token token = (Token) o;
      return Objects.equals(text, token.text);
    }

    @Override
    public int hashCode() {
      return Objects.hash(text);
    }

    @Override
    public String toString() {
      return getClass().getSimpleName() + '(' + text + ')';
    }
  }

  static class Quote extends Token {
    Quote() {
      super("'");
    }
  }

  public static class Punc extends Token {
    public Punc(char c) {
      super(String.valueOf(c));
    }
  }

  static class Space extends Token {
    Space() {
      super(" ");
    }

    @Override
    public String toString() {
      return " ";
    }
  }

  static class Newline extends Token {
    Newline() {
      super("\n");
    }

    @Override
    public String toString() {
      return getClass().getSimpleName() + "()";
    }
  }

  static class QuotedWord extends Token {
    QuotedWord(String text) {
      super(text);
    }
  }

  static class Word extends Token {
    Word(String text) {
      super(text);
    }
  }

  public static class IndexedParam extends Token {
    IndexedParam() {
      super("?");
    }
  }

  public static class NamedParam extends Token {
    NamedParam(String text) {
      super(text);
    }
  }

  /** Represents a named parameter that will be optimised for {@link java.sql.PreparedStatement} caching. */
  public static class OptimisedNamedParam extends NamedParam {
    OptimisedNamedParam(String text) {
      super(text);
    }
  }

}

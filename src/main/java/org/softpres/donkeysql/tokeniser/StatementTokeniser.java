/*
 * Copyright (c) 2017, Kieron Wilkinson
 */
package org.softpres.donkeysql.tokeniser;

import java.util.*;

/**
 * Determines positions of parameters within an SQL string, and allows
 * their replacement to translate to a standard JDBC string.
 */
public class StatementTokeniser {

  public static List<Token> tokenise(String statement) {
    State state = new State();

    for (char c : statement.toCharArray()) {
      switch (c) {
        case '\'':
          state.quote();
          break;
        case '(':
        case ')':
        case ',':
        case '=':
        case '>':
        case '<':
        case '+':
        case '-':
        case '*':
        case '/':
        case '%':
        case '!':
          state.punctuation(c);
          break;
        case ' ':
        case '\t':
          state.whitespace();
          break;
        case '?':
          state.questionMark();
          break;
        case ':':
          state.startNamedParam();
          break;
        default:
          state.continueWord(c);
      }
    }
    state.endWord();

    return state.tokens;
  }

  /** Mini state machine for decomposing SQL statements. */
  private static class State {
    private static final Quote QUOTE = new Quote();
    private static final Space SPACE = new Space();
    private static final IndexedParam INDEXED_PARAM = new IndexedParam();

    private final StringBuilder buffer = new StringBuilder();
    private final List<Token> tokens = new ArrayList<>();
    boolean quoting = false;
    boolean namedParam = false;

    private void quote() {
      if (quoting) {
        endWord();
      }
      tokens.add(QUOTE);
      quoting = !quoting;
    }

    private void punctuation(char c) {
      if (quoting) {
        buffer.append(c);
      } else {
        endWord();
        tokens.add(new Punc(c));
      }
    }

    private void whitespace() {
      endWord();
      tokens.add(SPACE);
    }

    private void questionMark() {
      if (quoting) {
        buffer.append("?"); // Push back
      } else {
        tokens.add(INDEXED_PARAM);
      }
    }

    private void startNamedParam() {
      if (quoting) {
        buffer.append(":"); // Push back
      } else {
        namedParam = true;
      }
    }

    private void endWord() {
      if (buffer.length() != 0) {
        String word = buffer.toString();
        buffer.setLength(0);
        if (namedParam) {
          tokens.add(new NamedParam(word));
        } else if (quoting) {
          tokens.add(new QuotedWord(word));
        } else {
          tokens.add(new Word(word));
        }
      }
      namedParam = false;
    }

    private void continueWord(char c) {
      buffer.append(c);
    }
  }
  
  public static abstract class Token {
    public final String text;

    public Token(String text) {
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

}

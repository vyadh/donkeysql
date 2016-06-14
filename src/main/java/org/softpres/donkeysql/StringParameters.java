/*
 * Copyright (c) 2015, Kieron Wilkinson
 */
package org.softpres.donkeysql;

import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * Determines positions of parameters within an SQL string, and allows
 * their replacement to translate to a standard JDBC string.
 */
class StringParameters {

  /**
   * Returns the parameter names specified in the statement at the appropriate positions
   * in the list, which allows supporting duplicate names in the statement.
   */
  static List<String> parameters(String statement) {
    return tokenise(statement).stream()
          .flatMap(token -> token instanceof Param ? Stream.of(token.text) : Stream.empty())
          .collect(toList());
  }

  /**
   * Replace all the parameters in an SQL statement with the standard question marks.
   */
  static String normalise(String statement) {
    return tokenise(statement).stream()
          .map(token -> token instanceof Param ? "?" : token.text)
          .collect(joining());
  }

  
  private static List<Token> tokenise(String statement) {
    State state = new State();

    for (char c : statement.toCharArray()) {
      switch (c) {
        case '\'':
          state.quote();
          break;
        case '(':
        case ')':
        case ',':
          state.punctuation(c);
          break;
        case ' ':
        case '\t':
          state.whitespace();
          break;
        case ':':
          state.startParam();
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

    StringBuilder buffer = new StringBuilder();
    boolean quoting = false;
    boolean param = false;
    List<Token> tokens = new ArrayList<>();

    private void quote() {
      if (quoting) {
        endWord();
      }
      tokens.add(QUOTE);
      quoting = !quoting;
    }

    private void punctuation(char c) {
      endWord();
      tokens.add(new Punc(c));
    }

    private void whitespace() {
      endWord();
      tokens.add(SPACE);
    }

    private void startParam() {
      if (!quoting) {
        param = true;
      } else {
        buffer.append(":"); // Push back
      }
    }

    private void endWord() {
      if (buffer.length() != 0) {
        String word = buffer.toString();
        buffer.setLength(0);
        if (param) {
          tokens.add(new Param(word));
        } else {
          tokens.add(quoting ? new QuotedWord(word) : new Word(word));
        }
      }
      param = false;
    }

    private void continueWord(char c) {
      buffer.append(c);
    }
  }
  
  private static abstract class Token {
    private String text;

    Token(String text) {
      this.text = text;
    }

    @Override
    public String toString() {
      return getClass().getSimpleName() + '(' + text + ')';
    }
  }
  
  private static class Quote extends Token {
    Quote() {
      super("'");
    }
  }
  private static class Punc extends Token {
    Punc(char c) {
      super(String.valueOf(c));
    }
  }
  private static class Space extends Token {
    Space() {
      super(" ");
    }

    @Override
    public String toString() {
      return " ";
    }
  }
  private static class QuotedWord extends Token {
    QuotedWord(String text) {
      super(text);
    }
  }
  private static class Word extends Token {
    Word(String text) {
      super(text);
    }
  }
  private static class Param extends Token {
    Param(String text) {
      super(text);
    }
  }
  
}

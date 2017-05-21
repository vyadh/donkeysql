/*
 * Copyright (c) 2015, Kieron Wilkinson
 */
package org.softpres.donkeysql.params;

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
          .flatMap(token -> token instanceof NamedParam ? Stream.of(token.text) : Stream.empty())
          .collect(toList());
  }

  /**
   * Replace all the parameters in an SQL statement with the standard question marks.
   */
  static String normalise(String statement) {
    return tokenise(statement).stream()
          .map(token -> token instanceof NamedParam ? "?" : token.text)
          .collect(joining());
  }


  static List<Token> tokenise(String statement) {
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

    StringBuilder buffer = new StringBuilder();
    boolean quoting = false;
    boolean namedParam = false;
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
        } else {
          tokens.add(quoting ? new QuotedWord(word) : new Word(word));
        }
      }
      namedParam = false;
    }

    private void continueWord(char c) {
      buffer.append(c);
    }
  }
  
  static abstract class Token {
    String text;

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
  static class IndexedParam extends Token {
    IndexedParam() {
      super("?");
    }
  }
  private static class NamedParam extends Token {
    NamedParam(String text) {
      super(text);
    }
  }

}

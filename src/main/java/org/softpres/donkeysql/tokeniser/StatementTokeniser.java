/*
 * Copyright (c) 2017, Kieron Wilkinson
 */
package org.softpres.donkeysql.tokeniser;

import java.util.*;

import org.softpres.donkeysql.tokeniser.Tokens.*;

/**
 * Simple tokeniser to determine positions of parameters within an SQL string,
 * allowing their replacement to translate to a standard JDBC string.
 *
 * If we need something more sophisticated we can migrate to a JavaCC scheme.
 *
 * Punctuation characters (aside from '_') are derived from Ron Savage's BNF grammar.
 * @see <a href="https://ronsavage.github.io/SQL/sql-2003-2.bnf.html">Ron Savage's BNF grammar</a>
 */
public class StatementTokeniser {

  public static List<Token> tokenise(String statement) {
    State state = new State();

    for (char c : statement.toCharArray()) {
      switch (c) {
        case '\'':
          state.quote();
          break;
        case '"':
        case '&':
        case '(':
        case ')':
        case '[':
        case ']':
        case '{':
        case '}':
        case ',':
        case '.':
        case ';':
        case '^':
        case '|':
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
          state.whitespace(c);
          break;
        case '\n':
        case '\r':
          state.newline(c);
          break;
        case '?':
          state.questionMark(c);
          break;
        case ':':
          state.colon(c);
          break;
        case '@':
          state.at(c);
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
    private static final Newline NEWLINE = new Newline();
    private static final IndexedParam INDEXED_PARAM = new IndexedParam();

    private final StringBuilder buffer = new StringBuilder();
    private final List<Token> tokens = new ArrayList<>();
    boolean quoting = false;
    boolean namedParam = false;
    boolean optimisedNamedParam = false;

    private void quote() {
      if (quoting) {
        endWord();
      }
      tokens.add(QUOTE);
      quoting = !quoting;
    }

    private void punctuation(char c) {
      if (!processQuoted(c)) {
        endWord();
        tokens.add(new Punc(c));
      }
    }

    private void whitespace(char c) {
      if (!processQuoted(c)) {
        endWord();
        tokens.add(SPACE);
      }
    }

    private void newline(char c) {
      if (!processQuoted(c)) {
        endWord();
        tokens.add(NEWLINE);
      }
    }

    private void questionMark(char c) {
      if (!processQuoted(c)) {
        tokens.add(INDEXED_PARAM);
      }
    }

    private void colon(char c) {
      if (!processQuoted(c)) {
        namedParam = true;
      }
    }

    private void at(char c) {
      if (!processQuoted(c)) {
        optimisedNamedParam = true;
      }
    }

    /** Provides a shorter syntax to consume special characters when quoted. */
    private boolean processQuoted(char c) {
      if (quoting) {
        buffer.append(c);
      }
      return quoting;
    }

    private void endWord() {
      if (buffer.length() != 0) {
        String word = buffer.toString();
        buffer.setLength(0);
        if (namedParam) {
          tokens.add(new NamedParam(word));
        } else if (optimisedNamedParam) {
          tokens.add(new OptimisedNamedParam(word));
        } else if (quoting) {
          tokens.add(new QuotedWord(word));
        } else {
          tokens.add(new Word(word));
        }
      }
      namedParam = false;
      optimisedNamedParam = false;
    }

    private void continueWord(char c) {
      buffer.append(c);
    }
  }
  
}

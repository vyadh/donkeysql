/*
 * Copyright (c) 2017, Kieron Wilkinson
 */
package org.softpres.donkeysql.params;

import org.softpres.donkeysql.tokeniser.StatementTokeniser;
import org.softpres.donkeysql.tokeniser.StatementTokeniser.IndexedParam;
import org.softpres.donkeysql.tokeniser.StatementTokeniser.NamedParam;
import org.softpres.donkeysql.tokeniser.StatementTokeniser.Punc;
import org.softpres.donkeysql.tokeniser.StatementTokeniser.Token;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * Associates a "named" parameterised SQL statement with the values to populate it.
 */
class NamedParamQuery implements ParamQuery {

  private static final IndexedParam INDEXED_PARAM = new IndexedParam();

  private final String sql;
  private final Map<String, Object> params;

  NamedParamQuery(String sql, Map<String, Object> params) {
    this.sql = sql;
    this.params = params;
  }

  @Override
  public PreparedStatement createStatement(Connection connection) throws SQLException {
    String normalisedSQL = normalise();
    PreparedStatement statement = connection.prepareStatement(normalisedSQL);
    applyParameters(statement);
    return statement;
  }

  /**
   * Replace all the named parameters in an SQL statement with the standard question marks.
   */
  String normalise() {
    return StatementTokeniser.tokenise(sql).stream()
          .flatMap(this::expand)
          .map(token -> token instanceof NamedParam ? INDEXED_PARAM : token)
          .map(token -> token.text)
          .collect(joining());
  }

  /** Repeat parameters when iterable value exists.  */
  private Stream<Token> expand(Token token) {
    if (token instanceof NamedParam) {
      Object value = value(token.text);
      if (value instanceof Iterable<?>) {
        return iterableAsParams(Streams.from((Iterable<?>)value));
      }
    }
    return Stream.of(token);
  }

  private Stream<Token> iterableAsParams(Stream<?> iterable) {
    return Streams.intersperse(
          iterable.map(any -> INDEXED_PARAM),
          new Punc(',')
    );
  }

  private void applyParameters(PreparedStatement statement) throws SQLException {
    List<Object> values = parameterValues(sql, params).collect(toList());

    for (int i = 0; i < values.size(); i++) {
      statement.setObject(i + 1, values.get(i));
    }
  }

  static Stream<Object> parameterValues(String statement, Map<String, Object> values) {
    return parameters(statement)
          .map(values::get)
          .flatMap(value -> value instanceof Iterable<?> ?
                Streams.from((Iterable<?>) value) :
                Stream.of(value));
  }

  /**
   * Returns the parameter names specified in the statement at the appropriate positions
   * in the list, which allows supporting duplicate names in the statement.
   */
  static Stream<String> parameters(String statement) {
    return StatementTokeniser.tokenise(statement).stream()
          .flatMap(token -> token instanceof NamedParam ?
                Stream.of(token.text) : Stream.empty());
  }

  private Object value(String name) {
    Object value = params.get(name);
    if (value == null) {
      throw new IllegalStateException("Unspecified parameter: " + name);
    }
    return value;
  }

  @Override
  public String toString() {
    return params.entrySet().stream().reduce(
          sql,
          this::replaceParam,
          (a, b) -> { throw new IllegalStateException(); } // Never hit, non-parallel
    );
  }

  private String replaceParam(String statement, Map.Entry<String, Object> entry) {
    return statement.replace(
          ':' + entry.getKey(),
          Humanise.paramValue(value(entry.getKey()))
    );
  }

}

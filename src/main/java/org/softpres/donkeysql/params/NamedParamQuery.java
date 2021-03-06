/*
 * Copyright (c) 2017, Kieron Wilkinson
 */
package org.softpres.donkeysql.params;

import org.softpres.donkeysql.tokeniser.StatementTokeniser;
import org.softpres.donkeysql.tokeniser.Tokens.NamedParam;
import org.softpres.donkeysql.tokeniser.Tokens.OptimisedNamedParam;
import org.softpres.donkeysql.tokeniser.Tokens.Punc;
import org.softpres.donkeysql.tokeniser.Tokens.Token;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * Associates a "named" parameterised SQL statement with the values to populate it.
 */
class NamedParamQuery implements ParamQuery {

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
    return normalisedTokens(sql, params::get)
          .map(token -> token.text)
          .collect(joining());
  }

  private static Stream<Token> normalisedTokens(String sql, Function<String, Object> lookupValue) {
    return StatementTokeniser.tokenise(sql).stream()
          .flatMap((Token token) -> expand(token, lookupValue));
  }

  /** Repeat parameters when iterable value exists.  */
  private static Stream<Token> expand(Token token, Function<String, Object> lookupValue) {
    if (token instanceof NamedParam) {
      Object value = lookupValue.apply(token.text);
      if (value instanceof Iterable<?>) {
        return expandedValues((Iterable<?>)value, token.getClass());
      } else {
        return Stream.of(new ValueParam(value));
      }
    }
    return Stream.of(token);
  }

  private static Stream<Token> expandedValues(Iterable<?> iterable, Class<? extends Token> type) {
    return Streams.intersperse(
          optimise(iterable, type).map(ValueParam::new),
          new Punc(',')
    );
  }

  /**
   * When indicated by use of the {@link OptimisedNamedParam} token, expand iterable values
   * to a power of 2 (by repeating the last element), giving SQL optimisers a better chance
   * of caching the {@link PreparedStatement} on IN operators (assumes users will only specify
   * when used on IN operator.
   */
  private static Stream<?> optimise(Iterable<?> iterable, Class<? extends Token> type) {
    if (type.equals(OptimisedNamedParam.class)) {
      List<?> list = Streams.list(iterable);
      return Streams.padWithLastTo(list, PowerOfTwo.nextOrZero(list.size()));
    } else {
      return Streams.from(iterable);
    }
  }

  private void applyParameters(PreparedStatement statement) throws SQLException {
    List<Object> values = parameterValues(sql, params).collect(toList());

    for (int i = 0; i < values.size(); i++) {
      statement.setObject(i + 1, values.get(i));
    }
  }

  static Stream<Object> parameterValues(String statement, Map<String, Object> values) {
    return normalisedTokens(statement, values::get)
          .filter(token -> token instanceof ValueParam)
          .map(token -> ((ValueParam)token).value);
  }

  @Override
  public String toString() {
    return normalisedTokens(sql, params::get)
          .map(this::humanise)
          .collect(joining());
  }

  private String humanise(Token token) {
    return token instanceof ValueParam ?
          Humanise.paramValue(((ValueParam)token).value) :
          token.text;
  }

  /** Represents a parameter with an associated value. */
  public static class ValueParam extends Token {
    final Object value;

    ValueParam(Object value) {
      super("?");
      this.value = value;
    }
  }

}

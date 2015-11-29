/*
 * Copyright (c) 2015, Kieron Wilkinson
 */
package org.softpres.donkeysql;

import java.util.*;

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
    return Arrays.stream(statement.split("[ \\(\\)]"))
          .map(String::trim)
          .filter(word -> word.startsWith(":"))
          .map(word -> word.substring(1))
          .collect(toList());
  }

  /**
   * Replace all the parameters in an SQL statement with the standard question marks.
   */
  static String normalise(String statement) {
    return statement.replaceAll(":[^\\s\\(\\)]+", "?");
  }

}

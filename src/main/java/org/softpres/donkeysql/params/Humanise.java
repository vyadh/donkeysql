/*
 * Copyright (c) 2017, Kieron Wilkinson
 */

package org.softpres.donkeysql.params;

import java.util.stream.Collectors;

/**
 * Provides utility functions used when formatting SQL statements for humans,
 * where it is used by multiple {@link ParamQuery} implementations.
 */
class Humanise {

  static String paramValue(Object param) {
    if (param == null) {
      return "NULL";
    } else if (param instanceof Number) {
      return param.toString();
    } else if (param instanceof Iterable<?>) {
      return Streams.from((Iterable<?>)param)
            .map(Humanise::paramValue)
            .collect(Collectors.joining(","));
    } else {
      return '\'' + param.toString() + '\'';
    }
  }

}

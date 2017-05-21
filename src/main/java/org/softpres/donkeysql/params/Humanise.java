/*
 * Copyright (c) 2017, Kieron Wilkinson
 */

package org.softpres.donkeysql.params;

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
    } else {
      return '\'' + param.toString() + '\'';
    }
  }

}

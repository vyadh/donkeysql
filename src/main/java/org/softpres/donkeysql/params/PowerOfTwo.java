/*
 * Copyright (c) 2017, Kieron Wilkinson
 */
package org.softpres.donkeysql.params;

/**
 * Facility to compute the next nearest power of two.
 */
class PowerOfTwo {

  /** Return the next power of two unless i=0, in which zero is returned. */
  static int nextOrZero(int i) {
    if (i == 0) {
      return 0;
    } else {
      return Math.max(1, Integer.highestOneBit(i - 1) << 1);
    }
  }

}

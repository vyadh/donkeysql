/*
 * Copyright (c) 2017, Kieron Wilkinson
 */
package org.softpres.donkeysql.params;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Small Stream utils.
 */
class Streams {

  static <T> Stream<T> from(Iterable<T> iterable) {
    return StreamSupport.stream(iterable.spliterator(), false);
  }

  /** Place separator between each element in the supplied stream. */
  static <T> Stream<T> intersperse(Stream<T> stream, T separator) {
    return stream
          .map(Stream::of)
          .reduce((a, b) -> Stream.concat(a, Stream.concat(Stream.of(separator), b)))
          .orElse(Stream.empty());
  }

}

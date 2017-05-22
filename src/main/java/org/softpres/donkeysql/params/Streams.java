/*
 * Copyright (c) 2017, Kieron Wilkinson
 */
package org.softpres.donkeysql.params;

import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;

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

  /**
   * Extend an iterable out to the specified size, padding with the last element.
   */
  static <T> Stream<T> padWithLastTo(Iterable<T> iterable, int size) {
    List<T> list = list(iterable);
    int extend = Math.max(0, size - list.size());
    return Stream.concat(list.stream(), lastRepeated(list).limit(extend));
  }

  /** Return {@link List} from iterable, or same list if no conversion needed. */
  static <T> List<T> list(Iterable<T> iterable) {
    if (iterable instanceof List<?>) {
      return (List<T>)iterable;
    } else {
      return from(iterable).collect(toList());
    }
  }

  /** Find the last element, and create an infinite stream of it. */
  private static <T> Stream<T> lastRepeated(List<T> list) {
    if (list.isEmpty()) {
      return Stream.empty();
    } else {
      T last = list.get(list.size() - 1);
      return Stream.iterate(last, UnaryOperator.identity());
    }
  }

}

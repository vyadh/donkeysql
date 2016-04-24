/*
 * Copyright (c) 2016, Kieron Wilkinson
 */
package org.softpres.donkeysql;

import java.util.Optional;

/**
 * Attempt closing of a resource, and return {@link Optional} containing
 * exception if one was thrown.
 */
class OptionalClose {

  static Optional<Exception> close(AutoCloseable closeable) {
    try {
      closeable.close();
      return Optional.empty();
    } catch (Exception e) {
      return Optional.of(e);
    }
  }

}

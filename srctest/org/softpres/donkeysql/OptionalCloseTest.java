/*
 * Copyright (c) 2016, Kieron Wilkinson
 */
package org.softpres.donkeysql;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OptionalClose}.
 */
public class OptionalCloseTest {

  @Test
  public void noneWhenNoException() {
    assertThat(OptionalClose.close(() -> {})).isEmpty();
  }

  @Test
  public void someWhenException() {
    Exception e = new IllegalStateException();
    assertThat(OptionalClose.close(() -> { throw e; })).contains(e);
  }

}

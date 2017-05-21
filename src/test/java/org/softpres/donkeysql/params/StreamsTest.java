/*
 * Copyright (c) 2017, Kieron Wilkinson
 */
package org.softpres.donkeysql.params;

import org.junit.Test;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.softpres.donkeysql.params.Streams.intersperse;

public class StreamsTest {

  @Test
  public void intersperseWithSeparator() {
    assertThat(intersperse(Stream.empty(), -1)).isEmpty();
    assertThat(intersperse(Stream.of(1), -1)).containsExactly(1);
    assertThat(intersperse(Stream.of(1, 2), 0)).containsExactly(1, 0, 2);
    assertThat(intersperse(Stream.of(1, 2, 3), 0)).containsExactly(1, 0, 2, 0, 3);
  }

}

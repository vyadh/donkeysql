/*
 * Copyright (c) 2017, Kieron Wilkinson
 */
package org.softpres.donkeysql.params;

import org.junit.Test;

import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.softpres.donkeysql.params.Streams.intersperse;
import static org.softpres.donkeysql.params.Streams.padWithLastTo;

public class StreamsTest {

  @Test
  public void intersperseWithSeparator() {
    assertThat(intersperse(Stream.empty(), -1)).isEmpty();
    assertThat(intersperse(Stream.of(1), -1)).containsExactly(1);
    assertThat(intersperse(Stream.of(1, 2), 0)).containsExactly(1, 0, 2);
    assertThat(intersperse(Stream.of(1, 2, 3), 0)).containsExactly(1, 0, 2, 0, 3);
  }

  @Test
  public void padWithLastWithEmptyIterable() {
    assertThat(padWithLastTo(emptyList(), 3)).isEmpty();
  }

  @Test
  public void padWithLastToSameSize() {
    assertThat(padWithLastTo(asList(1, 2, 3), 3)).containsOnly(1, 2, 3);
  }

  @Test
  public void padWithLessThanSize() {
    assertThat(padWithLastTo(asList(1, 2, 3), 1)).containsOnly(1, 2, 3);
  }

  @Test
  public void padToGreaterSize() {
    assertThat(padWithLastTo(singleton(1), 3)).containsOnly(1, 1, 1);
    assertThat(padWithLastTo(asList(2, 1), 3)).containsOnly(2, 1, 1);
    assertThat(padWithLastTo(asList(1, 2, 3), 5)).containsOnly(1, 2, 3, 3, 3);
    assertThat(padWithLastTo(asList(4, 5, 4), 6)).containsOnly(4, 5, 4, 4, 4, 4);
  }

}

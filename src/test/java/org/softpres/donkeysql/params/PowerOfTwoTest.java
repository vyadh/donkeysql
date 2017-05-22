/*
 * Copyright (c) 2017, Kieron Wilkinson
 */
package org.softpres.donkeysql.params;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PowerOfTwo}.
 */
public class PowerOfTwoTest {

  @Test
  public void zero() {
    assertThat(PowerOfTwo.nextOrZero(0)).isZero();
  }

  @Test
  public void one() {
    assertThat(PowerOfTwo.nextOrZero(1)).isOne();
  }

  @Test
  public void two() {
    assertThat(PowerOfTwo.nextOrZero(2)).isEqualTo(2);
  }

  @Test
  public void nextAfterPowerOfTwo() {
    assertThat(PowerOfTwo.nextOrZero(3)).isEqualTo(4);
    assertThat(PowerOfTwo.nextOrZero(5)).isEqualTo(8);
    assertThat(PowerOfTwo.nextOrZero(9)).isEqualTo(16);
  }

  @Test
  public void nextBeforePowerOfTwo() {
    assertThat(PowerOfTwo.nextOrZero(7)).isEqualTo(8);
    assertThat(PowerOfTwo.nextOrZero(15)).isEqualTo(16);
    assertThat(PowerOfTwo.nextOrZero(31)).isEqualTo(32);
  }

  @Test
  public void sameAsPowerOfTwo() {
    assertThat(PowerOfTwo.nextOrZero(32)).isEqualTo(32);
    assertThat(PowerOfTwo.nextOrZero(64)).isEqualTo(64);
    assertThat(PowerOfTwo.nextOrZero(128)).isEqualTo(128);
  }

}

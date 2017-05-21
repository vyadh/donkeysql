/*
 * Copyright (c) 2017, Kieron Wilkinson
 */
package org.softpres.donkeysql.params;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.softpres.donkeysql.params.Humanise.paramValue;

/**
 * Unit tests for {@link Humanise}.
 */
public class HumaniseTest {

  @Test
  public void humaniseWithNullParam() {
    assertThat(paramValue(null)).isEqualTo("NULL");
  }

  @Test
  public void humaniseWithNumericParam() {
    assertThat(paramValue(5)).isEqualTo("5");
    assertThat(paramValue(5.0)).isEqualTo("5.0");
    assertThat(paramValue(new BigDecimal("5.5"))).isEqualTo("5.5");
  }

  @Test
  public void humaniseWithStringParam() {
    assertThat(paramValue("value")).isEqualTo("'value'");
  }

  @Test
  public void humaniseWithObjectParam() {
    class Value {
      @Override
      public String toString() {
        return "val";
      }
    }

    assertThat(paramValue(new Value())).isEqualTo("'val'");
  }

}

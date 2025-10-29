/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;

/** Unit test for {@link TriState}. */
public class TriStateTest {

  @Test
  public void testIsSet() {
    assertThat(TriState.YES.isSet()).isTrue();
    assertThat(TriState.NO.isSet()).isTrue();
    assertThat(TriState.UNSET.isSet()).isFalse();
  }

  @Test
  public void testValueOf() {
    assertThat(TriState.valueOf(true)).isEqualTo(TriState.YES);
    assertThat(TriState.valueOf(false)).isEqualTo(TriState.NO);
  }

  @Test
  public void testAsBooleanValidValues() {
    assertThat(TriState.YES.asBoolean()).isTrue();
    assertThat(TriState.NO.asBoolean()).isFalse();
  }

  @Test
  public void testAsBooleanInvalidValues() {
    assertThatThrownBy(() -> TriState.UNSET.asBoolean()).isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void testAsBooleanDefault() {
    assertThat(TriState.YES.asBoolean(false)).isTrue();
    assertThat(TriState.NO.asBoolean(true)).isFalse();
    assertThat(TriState.UNSET.asBoolean(true)).isTrue();
    assertThat(TriState.UNSET.asBoolean(false)).isFalse();
  }

  @Test
  public void testAsBooleanObject() {
    assertThat(TriState.YES.asBooleanObject()).isSameAs(Boolean.TRUE);
    assertThat(TriState.NO.asBooleanObject()).isSameAs(Boolean.FALSE);
    assertThat(TriState.UNSET.asBooleanObject()).isNull();
  }
}

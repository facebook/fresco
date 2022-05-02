/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.util;

import static org.junit.Assert.*;

import org.junit.Test;

/** Unit test for {@link TriState}. */
public class TriStateTest {

  @Test
  public void testIsSet() {
    assertTrue(TriState.YES.isSet());
    assertTrue(TriState.NO.isSet());
    assertFalse(TriState.UNSET.isSet());
  }

  @Test
  public void testValueOf() {
    assertEquals(TriState.YES, TriState.valueOf(true));
    assertEquals(TriState.NO, TriState.valueOf(false));
  }

  @Test
  public void testAsBooleanValidValues() {
    assertTrue(TriState.YES.asBoolean());
    assertFalse(TriState.NO.asBoolean());
  }

  @Test(expected = IllegalStateException.class)
  public void testAsBooleanInvalidValues() {
    TriState.UNSET.asBoolean();
  }

  @Test
  public void testAsBooleanDefault() {
    assertTrue(TriState.YES.asBoolean(false));
    assertFalse(TriState.NO.asBoolean(true));
    assertTrue(TriState.UNSET.asBoolean(true));
    assertFalse(TriState.UNSET.asBoolean(false));
  }

  @Test
  public void testAsBooleanObject() {
    assertSame(Boolean.TRUE, TriState.YES.asBooleanObject());
    assertSame(Boolean.FALSE, TriState.NO.asBooleanObject());
    assertNull(TriState.UNSET.asBooleanObject());
  }
}

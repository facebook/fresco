/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.drawable;

import android.graphics.Matrix;
import android.graphics.PointF;
import org.junit.Assert;

/**
 * Test utils for android.graphics classes.
 */
public class AndroidGraphicsTestUtils {

  public static void assertEquals(Matrix expectedMatrix, Matrix actualMatrix) {
    if (expectedMatrix == null) {
      Assert.assertNull(actualMatrix);
      return;
    }
    Assert.assertNotNull(actualMatrix);
    String expected = expectedMatrix.toString();
    String actual = actualMatrix.toString();
    if (!actual.equals(expected)) {
      Assert.fail(String.format("\nexpected %s \nbut was %s", expected, actual));
    }
  }

  public static void assertEquals(PointF expected, PointF actual, float delta) {
    if (expected == null) {
      Assert.assertNull(actual);
      return;
    }
    Assert.assertEquals(expected.x, actual.x, delta);
    Assert.assertEquals(expected.y, actual.y, delta);
  }
}

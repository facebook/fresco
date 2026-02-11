/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.drawable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.within;

import android.graphics.Matrix;
import android.graphics.PointF;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

/** Test utils for android.graphics classes. */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class AndroidGraphicsTestUtils {

  public static void assertEquals(@Nullable Matrix expectedMatrix, Matrix actualMatrix) {
    if (expectedMatrix == null) {
      assertThat(actualMatrix).isNull();
      return;
    }
    assertThat(actualMatrix).isNotNull();
    String expected = expectedMatrix.toString();
    // NULLSAFE_FIXME[Not Vetted Third-Party]
    String actual = actualMatrix.toString();
    if (!actual.equals(expected)) {
      fail(String.format("\nexpected %s \nbut was %s", expected, actual));
    }
  }

  public static void assertEquals(@Nullable PointF expected, PointF actual, float delta) {
    if (expected == null) {
      assertThat(actual).isNull();
      return;
    }
    // NULLSAFE_FIXME[Not Vetted Third-Party]
    assertThat(actual.x).isCloseTo(expected.x, within(delta));
    // NULLSAFE_FIXME[Not Vetted Third-Party]
    assertThat(actual.y).isCloseTo(expected.y, within(delta));
  }
}

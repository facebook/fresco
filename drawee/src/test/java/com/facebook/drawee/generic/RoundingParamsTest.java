/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.generic;

import static org.assertj.core.api.Assertions.assertThat;

import android.graphics.Color;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class RoundingParamsTest {

  private RoundingParams mRoundingParams;

  @Before
  public void setUp() {
    mRoundingParams = new RoundingParams();
  }

  @Test
  public void testDefaults() {
    assertThat(mRoundingParams.getRoundingMethod())
        .isEqualTo(RoundingParams.RoundingMethod.BITMAP_ONLY);
    assertThat(mRoundingParams.getRoundAsCircle()).isFalse();
    assertThat(mRoundingParams.getCornersRadii()).isNull();
    assertThat(mRoundingParams.getOverlayColor()).isEqualTo(0);
    assertThat(mRoundingParams.getScaleDownInsideBorders()).isFalse();
    assertThat(mRoundingParams.getPaintFilterBitmap()).isFalse();
  }

  @Test
  public void testSetCircle() {
    assertThat(mRoundingParams.setRoundAsCircle(true)).isSameAs(mRoundingParams);
    assertThat(mRoundingParams.getRoundAsCircle()).isTrue();
    assertThat(mRoundingParams.setRoundAsCircle(false)).isSameAs(mRoundingParams);
    assertThat(mRoundingParams.getRoundAsCircle()).isFalse();
  }

  @Test
  public void testSetRadii() {
    mRoundingParams.setCornersRadius(9);
    assertThat(mRoundingParams.getCornersRadii())
        .containsExactly(new float[] {9, 9, 9, 9, 9, 9, 9, 9});

    mRoundingParams.setCornersRadii(8, 7, 2, 1);
    assertThat(mRoundingParams.getCornersRadii())
        .containsExactly(new float[] {8, 8, 7, 7, 2, 2, 1, 1});

    mRoundingParams.setCornersRadii(new float[] {1, 2, 3, 4, 5, 6, 7, 8});
    assertThat(mRoundingParams.getCornersRadii())
        .containsExactly(new float[] {1, 2, 3, 4, 5, 6, 7, 8});
  }

  @Test
  public void testSetRoundingMethod() {
    mRoundingParams.setRoundingMethod(RoundingParams.RoundingMethod.OVERLAY_COLOR);
    assertThat(mRoundingParams.getRoundingMethod())
        .isEqualTo(RoundingParams.RoundingMethod.OVERLAY_COLOR);
    mRoundingParams.setRoundingMethod(RoundingParams.RoundingMethod.BITMAP_ONLY);
    assertThat(mRoundingParams.getRoundingMethod())
        .isEqualTo(RoundingParams.RoundingMethod.BITMAP_ONLY);
  }

  @Test
  public void testSetOverlayColor() {
    mRoundingParams.setOverlayColor(0xC0123456);
    assertThat(mRoundingParams.getOverlayColor()).isEqualTo(0xC0123456);
    assertThat(mRoundingParams.getRoundingMethod())
        .isEqualTo(RoundingParams.RoundingMethod.OVERLAY_COLOR);
  }

  @Test
  public void testSetBorder() {
    int borderColor = Color.RED;
    float borderWidth = 0.8f;

    mRoundingParams.setBorder(borderColor, borderWidth);
    assertThat(mRoundingParams.getBorderColor()).isEqualTo(borderColor);
    assertThat(mRoundingParams.getBorderWidth()).isEqualTo(borderWidth);
  }

  @Test
  public void testSetScaleDownInsideBorders() {
    assertThat(mRoundingParams.setScaleDownInsideBorders(true)).isSameAs(mRoundingParams);
    assertThat(mRoundingParams.getScaleDownInsideBorders()).isTrue();
    assertThat(mRoundingParams.setScaleDownInsideBorders(false)).isSameAs(mRoundingParams);
    assertThat(mRoundingParams.getScaleDownInsideBorders()).isFalse();
  }

  @Test
  public void testSetPaintFilterBitmap() {
    assertThat(mRoundingParams.setPaintFilterBitmap(true)).isSameAs(mRoundingParams);
    assertThat(mRoundingParams.getPaintFilterBitmap()).isTrue();
    assertThat(mRoundingParams.setPaintFilterBitmap(false)).isSameAs(mRoundingParams);
    assertThat(mRoundingParams.getPaintFilterBitmap()).isFalse();
  }

  @Test
  public void testFactoryMethods() {
    RoundingParams params1 = RoundingParams.asCircle();
    assertThat(params1.getRoundAsCircle()).isTrue();

    RoundingParams params2 = RoundingParams.fromCornersRadius(9);
    assertThat(params2.getRoundAsCircle()).isFalse();
    assertThat(params2.getCornersRadii()).containsExactly(new float[] {9, 9, 9, 9, 9, 9, 9, 9});

    RoundingParams params3 = RoundingParams.fromCornersRadii(8, 7, 2, 1);
    assertThat(params3.getRoundAsCircle()).isFalse();
    assertThat(params3.getCornersRadii()).containsExactly(new float[] {8, 8, 7, 7, 2, 2, 1, 1});

    RoundingParams params4 = RoundingParams.fromCornersRadii(new float[] {1, 2, 3, 4, 5, 6, 7, 8});
    assertThat(params4.getRoundAsCircle()).isFalse();
    assertThat(params4.getCornersRadii()).containsExactly(new float[] {1, 2, 3, 4, 5, 6, 7, 8});
  }
}

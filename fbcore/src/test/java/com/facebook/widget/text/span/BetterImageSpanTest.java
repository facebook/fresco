/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.widget.text.span;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;

/**
 * Tests the dimensions assigned by {@link BetterImageSpan} ensuring the width/height of is
 * calculated correctly for different combinations of image and text height,
 * as well as span alignment.
 */
@RunWith(ParameterizedRobolectricTestRunner.class)
public class BetterImageSpanTest {

  private Drawable mDrawable;
  private final Rect mBounds = new Rect();
  private Paint.FontMetricsInt mFontMetrics = new Paint.FontMetricsInt();
  private String mDescription;
  private @BetterImageSpan.BetterImageSpanAlignment int mAlignment;
  private int mDrawableHeight;
  private final int mDrawableWidth = 100;
  private int mFontAscent;
  private int mFontDescent;
  private int mExpectedAscent;
  private int mExpectedDescent;
  private int mFontTop;
  private int mFontBottom;
  private int mExpectedTop;
  private int mExpectedBottom;

  @ParameterizedRobolectricTestRunner.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[][] {
          {
            "Center - small drawable",
            BetterImageSpan.ALIGN_CENTER,
            10,
            -20,
            10,
            -20,
            10,
            -25,
            15,
            -25,
            15
          },
          {
            "Center - large drawable",
            BetterImageSpan.ALIGN_CENTER,
            50,
            -20,
            10,
            -30,
            20,
            -25,
            15,
            -30,
            20
          },
          {
            "Baseline - small drawable",
            BetterImageSpan.ALIGN_BASELINE,
            10,
            -20,
            10,
            -20,
            10,
            -25,
            15,
            -25,
            15
          },
          {
            "Baseline - large drawable",
            BetterImageSpan.ALIGN_BASELINE,
            50,
            -20,
            10,
            -50,
            10,
            -25,
            15,
            -50,
            15
          },
          {
            "Bottom - small drawable",
            BetterImageSpan.ALIGN_BOTTOM,
            10,
            -20,
            10,
            -20,
            10,
            -25,
            15,
            -25,
            15
          },
          {
            "Bottom - large drawable",
            BetterImageSpan.ALIGN_BOTTOM,
            50,
            -20,
            10,
            -40,
            10,
            -25,
            15,
            -40,
            15
          }
        });
  }

  public BetterImageSpanTest(
      String description,
      int alignment,
      int drawableHeight,
      int fontAscent,
      int fontDescent,
      int expectedAscent,
      int expectedDescent,
      int fontTop,
      int fontBottom,
      int expectedTop,
      int expectedBottom) {
    mDescription = description;
    mAlignment = alignment;
    mDrawableHeight = drawableHeight;
    mFontAscent = fontAscent;
    mFontDescent = fontDescent;
    mExpectedAscent = expectedAscent;
    mExpectedDescent = expectedDescent;
    mFontTop = fontTop;
    mFontBottom = fontBottom;
    mExpectedTop = expectedTop;
    mExpectedBottom = expectedBottom;
  }

  @Before
  public void setup() {
    mDrawable = mock(Drawable.class);
    mBounds.set(0, 0, mDrawableWidth, mDrawableHeight);
    mFontMetrics.ascent = mFontAscent;
    mFontMetrics.descent = mFontDescent;
    mFontMetrics.top = mFontTop;
    mFontMetrics.bottom = mFontBottom;
    when(mDrawable.getBounds()).thenReturn(mBounds);
  }

  @Test
  public void testHeight() {
    BetterImageSpan span = new BetterImageSpan(mDrawable, mAlignment);
    span.getSize(null, null, 0, 0, mFontMetrics);
    assertThat(mFontMetrics.descent)
        .describedAs("Descent for " + mDescription)
        .isEqualTo(mExpectedDescent);
    assertThat(mFontMetrics.ascent)
        .describedAs("Ascent for " + mDescription)
        .isEqualTo(mExpectedAscent);
    assertThat(mFontMetrics.top).describedAs("Top for " + mDescription).isEqualTo(mExpectedTop);
    assertThat(mFontMetrics.bottom)
        .describedAs("Bottom for " + mDescription)
        .isEqualTo(mExpectedBottom);
  }

  @Test
  public void testWidth() {
    // The width stays consistent irrespective of alignment.
    BetterImageSpan span = new BetterImageSpan(mDrawable, mAlignment);
    int size = span.getSize(null, null, 0, 0, null);
    assertThat(size).isEqualTo(mDrawableWidth);
  }
}

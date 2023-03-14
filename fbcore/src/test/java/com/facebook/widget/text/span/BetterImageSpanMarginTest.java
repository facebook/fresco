/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.widget.text.span;

import static org.assertj.core.api.Assertions.assertThat;
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
 * calculated correctly for different combinations of image and text height, as well as span
 * alignment and image margin.
 */
@RunWith(ParameterizedRobolectricTestRunner.class)
public class BetterImageSpanMarginTest {

  private Drawable mDrawable;
  private final Rect mBounds = new Rect();
  private final Paint.FontMetricsInt mFontMetrics = new Paint.FontMetricsInt();
  private final String mDescription;
  private @BetterImageSpan.BetterImageSpanAlignment final int mAlignment;
  private final Rect mMargin;
  private final int mDrawableHeight;
  private final int mDrawableWidth = 100;
  private final int mFontAscent;
  private final int mFontDescent;
  private final int mExpectedAscent;
  private final int mExpectedDescent;
  private final int mFontTop;
  private final int mFontBottom;
  private final int mExpectedTop;
  private final int mExpectedBottom;

  @ParameterizedRobolectricTestRunner.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[][] {
          // -------------------------- Small drawable left/right margin --------------------------
          {
            "Center - small drawable - margin left",
            BetterImageSpan.ALIGN_CENTER,
            new Rect(
                10, // margin left
                0, // margin top
                0, // margin right
                0 // margin bottom
                ),
            10, // drawableHeight
            -20, // fontAscent
            10, // fontDescent
            -20, // expectedAscent
            10, // expectedDescent
            -25, // fontTop
            15, // fontBottom
            -25, // expectedTop
            15 // expectedBottom
          },
          {
            "Center - small drawable - margin right",
            BetterImageSpan.ALIGN_CENTER,
            new Rect(
                0, // margin left
                0, // margin top
                10, // margin right
                0 // margin bottom
                ),
            10, // drawableHeight
            -20, // fontAscent
            10, // fontDescent
            -20, // expectedAscent
            10, // expectedDescent
            -25, // fontTop
            15, // fontBottom
            -25, // expectedTop
            15 // expectedBottom
          },
          {
            "Center - small drawable - margin left and right",
            BetterImageSpan.ALIGN_CENTER,
            new Rect(
                5, // margin left
                0, // margin top
                5, // margin right
                0 // margin bottom
                ),
            10, // drawableHeight
            -20, // fontAscent
            10, // fontDescent
            -20, // expectedAscent
            10, // expectedDescent
            -25, // fontTop
            15, // fontBottom
            -25, // expectedTop
            15 // expectedBottom
          },
          {
            "Baseline - small drawable - margin left",
            BetterImageSpan.ALIGN_BASELINE,
            new Rect(
                10, // margin left
                0, // margin top
                0, // margin right
                0 // margin bottom
                ),
            10, // drawableHeight
            -20, // fontAscent
            10, // fontDescent
            -20, // expectedAscent
            10, // expectedDescent
            -25, // fontTop
            15, // fontBottom
            -25, // expectedTop
            15 // expectedBottom
          },
          {
            "Baseline - small drawable - margin right",
            BetterImageSpan.ALIGN_BASELINE,
            new Rect(
                0, // margin left
                0, // margin top
                10, // margin right
                0 // margin bottom
                ),
            10, // drawableHeight
            -20, // fontAscent
            10, // fontDescent
            -20, // expectedAscent
            10, // expectedDescent
            -25, // fontTop
            15, // fontBottom
            -25, // expectedTop
            15 // expectedBottom
          },
          {
            "Baseline - small drawable - margin left and right",
            BetterImageSpan.ALIGN_BASELINE,
            new Rect(
                5, // margin left
                0, // margin top
                5, // margin right
                0 // margin bottom
                ),
            10, // drawableHeight
            -20, // fontAscent
            10, // fontDescent
            -20, // expectedAscent
            10, // expectedDescent
            -25, // fontTop
            15, // fontBottom
            -25, // expectedTop
            15 // expectedBottom
          },
          {
            "Bottom - small drawable - margin left",
            BetterImageSpan.ALIGN_BOTTOM,
            new Rect(
                10, // margin left
                0, // margin top
                0, // margin right
                0 // margin bottom
                ),
            10, // drawableHeight
            -20, // fontAscent
            10, // fontDescent
            -20, // expectedAscent
            10, // expectedDescent
            -25, // fontTop
            15, // fontBottom
            -25, // expectedTop
            15 // expectedBottom
          },
          {
            "Bottom - small drawable - margin right",
            BetterImageSpan.ALIGN_BOTTOM,
            new Rect(
                0, // margin left
                0, // margin top
                10, // margin right
                0 // margin bottom
                ),
            10, // drawableHeight
            -20, // fontAscent
            10, // fontDescent
            -20, // expectedAscent
            10, // expectedDescent
            -25, // fontTop
            15, // fontBottom
            -25, // expectedTop
            15 // expectedBottom
          },
          {
            "Bottom - small drawable - margin left and right",
            BetterImageSpan.ALIGN_BOTTOM,
            new Rect(
                5, // margin left
                0, // margin top
                5, // margin right
                0 // margin bottom
                ),
            10, // drawableHeight
            -20, // fontAscent
            10, // fontDescent
            -20, // expectedAscent
            10, // expectedDescent
            -25, // fontTop
            15, // fontBottom
            -25, // expectedTop
            15 // expectedBottom
          },
          // -------------------------- Large drawable left/right margin --------------------------
          {
            "Center - large drawable - margin left",
            BetterImageSpan.ALIGN_CENTER,
            new Rect(
                10, // margin left
                0, // margin top
                0, // margin right
                0 // margin bottom
                ),
            50, // drawableHeight
            -20, // fontAscent
            10, // fontDescent
            -30, // expectedAscent
            20, // expectedDescent
            -25, // fontTop
            15, // fontBottom
            -30, // expectedTop
            20 // expectedBottom
          },
          {
            "Center - large drawable - margin right",
            BetterImageSpan.ALIGN_CENTER,
            new Rect(
                0, // margin left
                0, // margin top
                10, // margin right
                0 // margin bottom
                ),
            50, // drawableHeight
            -20, // fontAscent
            10, // fontDescent
            -30, // expectedAscent
            20, // expectedDescent
            -25, // fontTop
            15, // fontBottom
            -30, // expectedTop
            20 // expectedBottom
          },
          {
            "Center - large drawable - margin left and right",
            BetterImageSpan.ALIGN_CENTER,
            new Rect(
                5, // margin left
                0, // margin top
                5, // margin right
                0 // margin bottom
                ),
            50, // drawableHeight
            -20, // fontAscent
            10, // fontDescent
            -30, // expectedAscent
            20, // expectedDescent
            -25, // fontTop
            15, // fontBottom
            -30, // expectedTop
            20 // expectedBottom
          },
          {
            "Baseline - large drawable - margin left",
            BetterImageSpan.ALIGN_BASELINE,
            new Rect(
                10, // margin left
                0, // margin top
                0, // margin right
                0 // margin bottom
                ),
            50, // drawableHeight
            -20, // fontAscent
            10, // fontDescent
            -50, // expectedAscent
            10, // expectedDescent
            -25, // fontTop
            15, // fontBottom
            -50, // expectedTop
            15 // expectedBottom
          },
          {
            "Baseline - large drawable - margin right",
            BetterImageSpan.ALIGN_BASELINE,
            new Rect(
                0, // margin left
                0, // margin top
                10, // margin right
                0 // margin bottom
                ),
            50, // drawableHeight
            -20, // fontAscent
            10, // fontDescent
            -50, // expectedAscent
            10, // expectedDescent
            -25, // fontTop
            15, // fontBottom
            -50, // expectedTop
            15 // expectedBottom
          },
          {
            "Baseline - large drawable - margin left and right",
            BetterImageSpan.ALIGN_BASELINE,
            new Rect(
                5, // margin left
                0, // margin top
                5, // margin right
                0 // margin bottom
                ),
            50, // drawableHeight
            -20, // fontAscent
            10, // fontDescent
            -50, // expectedAscent
            10, // expectedDescent
            -25, // fontTop
            15, // fontBottom
            -50, // expectedTop
            15 // expectedBottom
          },
          {
            "Bottom - large drawable - margin left",
            BetterImageSpan.ALIGN_BOTTOM,
            new Rect(
                10, // margin left
                0, // margin top
                0, // margin right
                0 // margin bottom
                ),
            50, // drawableHeight
            -20, // fontAscent
            10, // fontDescent
            -40, // expectedAscent
            10, // expectedDescent
            -25, // fontTop
            15, // fontBottom
            -40, // expectedTop
            15 // expectedBottom
          },
          {
            "Bottom - large drawable - margin right",
            BetterImageSpan.ALIGN_BOTTOM,
            new Rect(
                0, // margin left
                0, // margin top
                10, // margin right
                0 // margin bottom
                ),
            50, // drawableHeight
            -20, // fontAscent
            10, // fontDescent
            -40, // expectedAscent
            10, // expectedDescent
            -25, // fontTop
            15, // fontBottom
            -40, // expectedTop
            15 // expectedBottom
          },
          {
            "Bottom - large drawable - margin left and right",
            BetterImageSpan.ALIGN_BOTTOM,
            new Rect(
                5, // margin left
                0, // margin top
                5, // margin right
                0 // margin bottom
                ),
            50, // drawableHeight
            -20, // fontAscent
            10, // fontDescent
            -40, // expectedAscent
            10, // expectedDescent
            -25, // fontTop
            15, // fontBottom
            -40, // expectedTop
            15 // expectedBottom
          },
          // -------------------------- Small drawable top/bottom margin --------------------------
          {
            "Center - small drawable - margin top",
            BetterImageSpan.ALIGN_CENTER,
            new Rect(
                0, // margin left
                10, // margin top
                0, // margin right
                0 // margin bottom
                ),
            10, // drawableHeight
            -20, // fontAscent
            10, // fontDescent
            -20, // expectedAscent
            10, // expectedDescent
            -25, // fontTop
            15, // fontBottom
            -25, // expectedTop
            15 // expectedBottom
          },
          {
            "Center - small drawable - margin bottom",
            BetterImageSpan.ALIGN_CENTER,
            new Rect(
                0, // margin left
                0, // margin top
                0, // margin right
                10 // margin bottom
                ),
            10, // drawableHeight
            -20, // fontAscent
            10, // fontDescent
            -20, // expectedAscent
            10, // expectedDescent
            -25, // fontTop
            15, // fontBottom
            -25, // expectedTop
            15 // expectedBottom
          },
          {
            "Center - small drawable - margin top and bottom",
            BetterImageSpan.ALIGN_CENTER,
            new Rect(
                0, // margin left
                5, // margin top
                0, // margin right
                5 // margin bottom
                ),
            10, // drawableHeight
            -20, // fontAscent
            10, // fontDescent
            -20, // expectedAscent
            10, // expectedDescent
            -25, // fontTop
            15, // fontBottom
            -25, // expectedTop
            15 // expectedBottom
          },
          {
            "Baseline - small drawable - margin top",
            BetterImageSpan.ALIGN_BASELINE,
            new Rect(
                0, // margin left
                10, // margin top
                0, // margin right
                0 // margin bottom
                ),
            10, // drawableHeight
            -20, // fontAscent
            10, // fontDescent
            -20, // expectedAscent
            10, // expectedDescent
            -25, // fontTop
            15, // fontBottom
            -25, // expectedTop
            15 // expectedBottom
          },
          {
            "Baseline - small drawable - margin bottom",
            BetterImageSpan.ALIGN_BASELINE,
            new Rect(
                0, // margin left
                0, // margin top
                0, // margin right
                10 // margin bottom
                ),
            10, // drawableHeight
            -20, // fontAscent
            10, // fontDescent
            -20, // expectedAscent
            10, // expectedDescent
            -25, // fontTop
            15, // fontBottom
            -25, // expectedTop
            15 // expectedBottom
          },
          {
            "Baseline - small drawable - margin top and bottom",
            BetterImageSpan.ALIGN_BASELINE,
            new Rect(
                0, // margin left
                5, // margin top
                0, // margin right
                5 // margin bottom
                ),
            10, // drawableHeight
            -20, // fontAscent
            10, // fontDescent
            -20, // expectedAscent
            10, // expectedDescent
            -25, // fontTop
            15, // fontBottom
            -25, // expectedTop
            15 // expectedBottom
          },
          {
            "Bottom - small drawable - margin top",
            BetterImageSpan.ALIGN_BOTTOM,
            new Rect(
                0, // margin left
                10, // margin top
                0, // margin right
                0 // margin bottom
                ),
            10, // drawableHeight
            -20, // fontAscent
            10, // fontDescent
            -20, // expectedAscent
            10, // expectedDescent
            -25, // fontTop
            15, // fontBottom
            -25, // expectedTop
            15 // expectedBottom
          },
          {
            "Bottom - small drawable - margin bottom",
            BetterImageSpan.ALIGN_BOTTOM,
            new Rect(
                0, // margin left
                0, // margin top
                0, // margin right
                10 // margin bottom
                ),
            10, // drawableHeight
            -20, // fontAscent
            10, // fontDescent
            -20, // expectedAscent
            10, // expectedDescent
            -25, // fontTop
            15, // fontBottom
            -25, // expectedTop
            15 // expectedBottom
          },
          {
            "Bottom - small drawable - margin top and bottom",
            BetterImageSpan.ALIGN_BOTTOM,
            new Rect(
                0, // margin left
                5, // margin top
                0, // margin right
                5 // margin bottom
                ),
            10, // drawableHeight
            -20, // fontAscent
            10, // fontDescent
            -20, // expectedAscent
            10, // expectedDescent
            -25, // fontTop
            15, // fontBottom
            -25, // expectedTop
            15 // expectedBottom
          },
          // -------------------------- Large drawable top/bottom margin --------------------------
          {
            "Center - large drawable - margin top",
            BetterImageSpan.ALIGN_CENTER,
            new Rect(
                0, // margin left
                10, // margin top
                0, // margin right
                0 // margin bottom
                ),
            50, // drawableHeight
            -20, // fontAscent
            10, // fontDescent
            -35, // expectedAscent
            25, // expectedDescent
            -25, // fontTop
            15, // fontBottom
            -35, // expectedTop
            25, // expectedBottom
          },
          {
            "Center - large drawable - margin bottom",
            BetterImageSpan.ALIGN_CENTER,
            new Rect(
                0, // margin left
                0, // margin top
                0, // margin right
                10 // margin bottom
                ),
            50, // drawableHeight
            -20, // fontAscent
            10, // fontDescent
            -35, // expectedAscent
            25, // expectedDescent
            -25, // fontTop
            15, // fontBottom
            -35, // expectedTop
            25, // expectedBottom
          },
          {
            "Center - large drawable - margin top and bottom",
            BetterImageSpan.ALIGN_CENTER,
            new Rect(
                0, // margin left
                5, // margin top
                0, // margin right
                5 // margin bottom
                ),
            50, // drawableHeight
            -20, // fontAscent
            10, // fontDescent
            -35, // expectedAscent
            25, // expectedDescent
            -25, // fontTop
            15, // fontBottom
            -35, // expectedTop
            25, // expectedBottom
          },
          {
            "Baseline - large drawable - margin top",
            BetterImageSpan.ALIGN_BASELINE,
            new Rect(
                0, // margin left
                10, // margin top
                0, // margin right
                0 // margin bottom
                ),
            50, // drawableHeight
            -20, // fontAscent
            10, // fontDescent
            -60, // expectedAscent
            10, // expectedDescent
            -25, // fontTop
            15, // fontBottom
            -60, // expectedTop
            15 // expectedBottom
          },
          {
            "Baseline - large drawable - margin bottom",
            BetterImageSpan.ALIGN_BASELINE,
            new Rect(
                0, // margin left
                0, // margin top
                0, // margin right
                10 // margin bottom
                ),
            50, // drawableHeight
            -20, // fontAscent
            10, // fontDescent
            -60, // expectedAscent
            10, // expectedDescent
            -25, // fontTop
            15, // fontBottom
            -60, // expectedTop
            15 // expectedBottom
          },
          {
            "Baseline - large drawable - margin top and bottom",
            BetterImageSpan.ALIGN_BASELINE,
            new Rect(
                0, // margin left
                5, // margin top
                0, // margin right
                5 // margin bottom
                ),
            50, // drawableHeight
            -20, // fontAscent
            10, // fontDescent
            -60, // expectedAscent
            10, // expectedDescent
            -25, // fontTop
            15, // fontBottom
            -60, // expectedTop
            15 // expectedBottom
          },
          {
            "Bottom - large drawable - margin top",
            BetterImageSpan.ALIGN_BOTTOM,
            new Rect(
                0, // margin left
                10, // margin top
                0, // margin right
                0 // margin bottom
                ),
            50, // drawableHeight
            -20, // fontAscent
            10, // fontDescent
            -50, // expectedAscent
            10, // expectedDescent
            -25, // fontTop
            15, // fontBottom
            -50, // expectedTop
            15 // expectedBottom
          },
          {
            "Bottom - large drawable - margin bottom",
            BetterImageSpan.ALIGN_BOTTOM,
            new Rect(
                0, // margin left
                0, // margin top
                0, // margin right
                10 // margin bottom
                ),
            50, // drawableHeight
            -20, // fontAscent
            10, // fontDescent
            -50, // expectedAscent
            10, // expectedDescent
            -25, // fontTop
            15, // fontBottom
            -50, // expectedTop
            15 // expectedBottom
          },
          {
            "Bottom - large drawable - margin top and bottom",
            BetterImageSpan.ALIGN_BOTTOM,
            new Rect(
                0, // margin left
                5, // margin top
                0, // margin right
                5 // margin bottom
                ),
            50, // drawableHeight
            -20, // fontAscent
            10, // fontDescent
            -50, // expectedAscent
            10, // expectedDescent
            -25, // fontTop
            15, // fontBottom
            -50, // expectedTop
            15 // expectedBottom
          }
        });
  }

  public BetterImageSpanMarginTest(
      String description,
      int alignment,
      Rect margin,
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
    mMargin = margin;
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
    BetterImageSpan span = new BetterImageSpan(mDrawable, mAlignment, mMargin);
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
    // The width consists of the drawable width and horizontal margin.
    BetterImageSpan span = new BetterImageSpan(mDrawable, mAlignment, mMargin);
    int size = span.getSize(null, null, 0, 0, null);
    assertThat(size).isEqualTo(mDrawableWidth + mMargin.left + mMargin.right);
  }
}

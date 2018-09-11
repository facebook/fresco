/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */


package com.facebook.drawee.span;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import com.facebook.drawee.view.DraweeHolder;
import com.facebook.widget.text.span.BetterImageSpan;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

/**
 * Tests {@link DraweeSpanStringBuilder}
 */
@RunWith(RobolectricTestRunner.class)
public class DraweeSpanStringBuilderTest {

  private static final String TEXT = "ABCDEFG";
  private static final int DRAWABLE_WIDTH = 10;
  private static final int DRAWABLE_HEIGHT = 32;

  @Mock public DraweeHolder mDraweeHolder;
  @Mock public Drawable mTopLevelDrawable;
  @Mock public Rect mDrawableBounds;
  @Mock public View mView;

  private DraweeSpanStringBuilder mDraweeSpanStringBuilder;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    when(mDraweeHolder.getTopLevelDrawable()).thenReturn(mTopLevelDrawable);
    when(mTopLevelDrawable.getBounds()).thenReturn(mDrawableBounds);
    when(mDrawableBounds.width()).thenReturn(0);
    when(mDrawableBounds.height()).thenReturn(0);
    mDraweeSpanStringBuilder = new DraweeSpanStringBuilder(TEXT);
  }

  @Test
  public void testTextCorrect() {
    assertThat(mDraweeSpanStringBuilder.toString()).isEqualTo(TEXT);
  }

  @Test
  public void testNoDraweeSpan() {
    assertThat(mDraweeSpanStringBuilder.hasDraweeSpans()).isFalse();
  }

  @Test
  public void testDraweeSpanAdded() {
    addDraweeSpan(mDraweeSpanStringBuilder, mDraweeHolder, 3, 1);

    assertThat(mDraweeSpanStringBuilder.toString()).isEqualTo(TEXT);
    assertThat(mDraweeSpanStringBuilder.hasDraweeSpans()).isTrue();
  }

  @Test
  public void testLifecycle() {
    addDraweeSpan(mDraweeSpanStringBuilder, mDraweeHolder, 3, 1);

    mDraweeSpanStringBuilder.onAttach();
    verify(mDraweeHolder).onAttach();

    mDraweeSpanStringBuilder.onDetach();
    verify(mDraweeHolder).onDetach();
  }

  @Test
  public void testDraweeSpanInSpannable() {
    addDraweeSpan(mDraweeSpanStringBuilder, mDraweeHolder, 3, 1);
    DraweeSpan[] draweeSpans = mDraweeSpanStringBuilder.getSpans(
        0,
        mDraweeSpanStringBuilder.length(),
        DraweeSpan.class);

    assertThat(draweeSpans).hasSize(1);
    assertThat(draweeSpans[0].getDrawable()).isEqualTo(mTopLevelDrawable);
  }

  private static void addDraweeSpan(
      DraweeSpanStringBuilder draweeSpanStringBuilder,
      DraweeHolder draweeHolder,
      int index,
      int spanLength) {
    draweeSpanStringBuilder.setImageSpan(
        draweeHolder, /* draweeHolder */
        index, /* startIndex */
        index + spanLength, /* endIndex */
        DRAWABLE_WIDTH, /* drawableWidthPx */
        DRAWABLE_HEIGHT, /* drawableHeightPx */
        false, /* enableResizing */
        BetterImageSpan.ALIGN_CENTER); /* verticalAlignment */
  }
}

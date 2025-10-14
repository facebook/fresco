/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.span;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import com.facebook.drawee.view.DraweeHolder;
import com.facebook.fresco.vito.core.impl.KFrescoVitoDrawable;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.source.ImageSource;
import com.facebook.fresco.vito.textspan.VitoSpan;
import com.facebook.fresco.vito.textspan.VitoSpanLoader;
import com.facebook.widget.text.span.BetterImageSpan;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

/** Tests {@link DraweeSpanStringBuilder} */
@RunWith(RobolectricTestRunner.class)
public class DraweeSpanStringBuilderTest {

  private static final String TEXT = "ABCDEFG";
  private static final int DRAWABLE_WIDTH = 10;
  private static final int DRAWABLE_HEIGHT = 32;

  @Mock public DraweeHolder mDraweeHolder;
  @Mock public ImageSource mImageSource;
  @Mock public ImageOptions mImageOptions;

  @Mock(answer = Answers.RETURNS_MOCKS)
  public VitoSpanLoader mVitoSpanLoader;

  @Mock(answer = Answers.RETURNS_MOCKS)
  public Context mContext;

  @Mock public Drawable mTopLevelDrawable;
  @Mock public KFrescoVitoDrawable mDrawableInterface;
  @Mock public Rect mDrawableBounds;
  @Mock public View mView;

  private DraweeSpanStringBuilder mDraweeSpanStringBuilder;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    when(mVitoSpanLoader.createDrawable()).thenReturn(mDrawableInterface);
    //    when((Drawable) mDrawableInterface).thenReturn(mTopLevelDrawable);
    when(mDrawableInterface.getBounds()).thenReturn(mDrawableBounds);
    when(mDraweeHolder.getTopLevelDrawable()).thenReturn(mTopLevelDrawable);
    when(mTopLevelDrawable.getBounds()).thenReturn(mDrawableBounds);
    when(mDrawableBounds.width()).thenReturn(0);
    when(mDrawableBounds.height()).thenReturn(0);
    mDraweeSpanStringBuilder = spy(new DraweeSpanStringBuilder(TEXT));
    doNothing().when(mDraweeSpanStringBuilder).vitoSpanShow(any(), any(), any(), any(), any());
  }

  @Test
  public void testTextCorrect() {
    assertThat(mDraweeSpanStringBuilder.toString()).isEqualTo(TEXT);
  }

  @Test
  public void testNoDraweeSpan() {
    assertThat(mDraweeSpanStringBuilder.hasVitoSpans()).isFalse();
  }

  @Test
  public void testDraweeSpanAdded() {
    addDraweeSpan(
        mDraweeSpanStringBuilder, mContext, mVitoSpanLoader, mImageSource, mImageOptions, 3, 1);

    assertThat(mDraweeSpanStringBuilder.toString()).isEqualTo(TEXT);
    assertThat(mDraweeSpanStringBuilder.hasVitoSpans()).isTrue();
  }

  @Test
  public void testDraweeSpanInSpannable() {
    addDraweeSpan(
        mDraweeSpanStringBuilder, mContext, mVitoSpanLoader, mImageSource, mImageOptions, 3, 1);
    VitoSpan[] draweeSpans =
        mDraweeSpanStringBuilder.getSpans(0, mDraweeSpanStringBuilder.length(), VitoSpan.class);

    assertThat(draweeSpans).hasSize(1);
    assertThat(draweeSpans[0].getDrawableInterface()).isEqualTo(mDrawableInterface);
  }

  private static void addDraweeSpan(
      DraweeSpanStringBuilder draweeSpanStringBuilder,
      Context context,
      VitoSpanLoader vitoSpanLoader,
      ImageSource imageSource,
      ImageOptions imageOptions,
      int index,
      int spanLength) {
    draweeSpanStringBuilder.setImageVitoSpan(
        context,
        vitoSpanLoader,
        imageSource,
        imageOptions,
        "this is anything",
        index, /* startIndex */
        index + spanLength, /* endIndex */
        DRAWABLE_WIDTH, /* drawableWidthPx */
        DRAWABLE_HEIGHT, /* drawableHeightPx */
        false, /* enableResizing */
        BetterImageSpan.ALIGN_CENTER); /* verticalAlignment */
  }
}

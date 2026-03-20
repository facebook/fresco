/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.span;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import com.facebook.drawee.view.DraweeHolder;
import com.facebook.widget.text.span.BetterImageSpan;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

/** Tests {@link DraweeSpan} */
@RunWith(RobolectricTestRunner.class)
public class DraweeSpanTest {

  private DraweeHolder mDraweeHierarchy;
  @Mock public Rect mBounds;
  @Mock public Drawable mDrawable;

  private DraweeSpan mDraweeSpan;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    mDraweeHierarchy = spy(new DraweeHolder(null));
    doReturn(mDrawable).when(mDraweeHierarchy).getTopLevelDrawable();
    when(mDrawable.getBounds()).thenReturn(mBounds);
    mDraweeSpan = new DraweeSpan(mDraweeHierarchy, BetterImageSpan.ALIGN_CENTER);
  }

  @Test
  public void testLifecycle() {
    DraweeHolder holder = mDraweeSpan.getDraweeHolder();
    assertThat(mDraweeSpan.isAttached()).isFalse();

    mDraweeSpan.onAttach();
    verify(mDraweeHierarchy).onAttach();
    assertThat(mDraweeSpan.isAttached()).isTrue();
    assertThat(holder.isAttached()).isTrue();

    mDraweeSpan.onDetach();
    verify(mDraweeHierarchy).onDetach();
    assertThat(mDraweeSpan.isAttached()).isFalse();
    assertThat(holder.isAttached()).isFalse();
  }

  @Test
  public void testGetDrawable() {
    Drawable drawable = mDraweeSpan.getDrawable();
    verify(mDraweeHierarchy).getTopLevelDrawable();
    assertThat(drawable).isEqualTo(mDrawable);
  }
}

/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */


package com.facebook.drawee.span;

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

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link DraweeSpan}
 */
@RunWith(RobolectricTestRunner.class)
public class DraweeSpanTest {

  @Mock public DraweeHolder mDraweeHierarchy;
  @Mock public Rect mBounds;
  @Mock public Drawable mDrawable;

  private DraweeSpan mDraweeSpan;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    when(mDraweeHierarchy.getTopLevelDrawable()).thenReturn(mDrawable);
    when(mDrawable.getBounds()).thenReturn(mBounds);
    mDraweeSpan = new DraweeSpan(mDraweeHierarchy, BetterImageSpan.ALIGN_CENTER);
  }

  @Test
  public void testLifecycle() {
    mDraweeSpan.onAttach();
    verify(mDraweeHierarchy).onAttach();

    mDraweeSpan.onDetach();
    verify(mDraweeHierarchy).onDetach();
  }

  @Test
  public void testGetDrawable() {
    Drawable drawable = mDraweeSpan.getDrawable();
    verify(mDraweeHierarchy).getTopLevelDrawable();
    assertThat(drawable).isEqualTo(mDrawable);
  }
}

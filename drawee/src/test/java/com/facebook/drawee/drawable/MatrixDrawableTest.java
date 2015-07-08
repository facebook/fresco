/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawee.drawable;

import android.graphics.Matrix;
import android.graphics.drawable.Drawable;

import org.robolectric.RobolectricTestRunner;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class MatrixDrawableTest {
  private Drawable mUnderlyingDrawable;
  private Matrix mMatrix1;
  private Matrix mMatrix2;

  private MatrixDrawable mMatrixDrawable;

  @Before
  public void setUp() {
    mUnderlyingDrawable = mock(Drawable.class);
    mMatrix1 = mock(Matrix.class);
    mMatrix2 = mock(Matrix.class);
    mMatrixDrawable = new MatrixDrawable(mUnderlyingDrawable, mMatrix1);
  }

  @Test
  public void testIntrinsicDimensions() {
    when(mUnderlyingDrawable.getIntrinsicWidth()).thenReturn(100);
    when(mUnderlyingDrawable.getIntrinsicHeight()).thenReturn(200);
    Assert.assertEquals(100, mMatrixDrawable.getIntrinsicWidth());
    Assert.assertEquals(200, mMatrixDrawable.getIntrinsicHeight());
  }

  @Test
  public void testSetMatrix() throws Exception {
    // initial state
    Assert.assertEquals(mUnderlyingDrawable, mMatrixDrawable.getCurrent());
    Assert.assertEquals(mMatrixDrawable.getMatrix(), mMatrix1);

    mMatrixDrawable.setMatrix(mMatrix2);
    Assert.assertEquals(mUnderlyingDrawable, mMatrixDrawable.getCurrent());
    Assert.assertEquals(mMatrixDrawable.getMatrix(), mMatrix2);
  }

}

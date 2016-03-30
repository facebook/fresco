/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.drawee.drawable;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link OrientedDrawable}
 */
@RunWith(RobolectricTestRunner.class)
public class OrientedDrawableTest {

  private Drawable mDrawable;
  private Canvas mCanvas;
  private Rect mBounds;

  @Before
  public void setUp() {
    mDrawable = mock(Drawable.class);
    mCanvas = mock(Canvas.class);
    mBounds = mock(Rect.class);

    // Change the bounds so that they will be different from the Drawable initial bounds and
    // setBounds calls onBoundsChange.
    mBounds.left = 100;
    mBounds.top = 100;
    mBounds.right = 500;
    mBounds.bottom = 500;
  }

  @Test
  public void testCreation_invalidAngle() {
    try {
      new OrientedDrawable(mDrawable, 20);
      fail();
    } catch (IllegalArgumentException e) {
      // Do nothing, expected.
    }
  }

  @Test
  public void testCreation_zeroDegrees() {
    OrientedDrawable drawable = new OrientedDrawable(mDrawable, 0);
    drawable.setBounds(mBounds);
    drawable.draw(mCanvas);
    assertTrue(drawable.mRotationMatrix.isIdentity());
    verify(mDrawable).setBounds(new Rect(mBounds));
  }

  @Test
  public void testCreation_nintyDegrees() {
    OrientedDrawable drawable = new OrientedDrawable(mDrawable, 90);
    drawable.setBounds(mBounds);
    drawable.draw(mCanvas);

    Matrix expectedMatrix = new Matrix();
    expectedMatrix.setRotate(90, drawable.getBounds().centerX(), drawable.getBounds().centerY());
    assertFalse(drawable.mRotationMatrix.isIdentity());
    AndroidGraphicsTestUtils.assertEquals(expectedMatrix, drawable.mRotationMatrix);
    verifySetBounds(expectedMatrix);
  }

  @Test
  public void testCreation_hundredAndEightyDegrees() {
    OrientedDrawable drawable = new OrientedDrawable(mDrawable, 180);
    drawable.setBounds(mBounds);
    drawable.draw(mCanvas);

    Matrix expectedMatrix = new Matrix();
    expectedMatrix.setRotate(180, drawable.getBounds().centerX(), drawable.getBounds().centerY());
    assertFalse(drawable.mRotationMatrix.isIdentity());
    AndroidGraphicsTestUtils.assertEquals(expectedMatrix, drawable.mRotationMatrix);
    verifySetBounds(expectedMatrix);
  }

  @Test
  public void testCreation_twoHundredAndSeventyDegrees() {
    OrientedDrawable drawable = new OrientedDrawable(mDrawable, 270);
    drawable.setBounds(mBounds);
    drawable.draw(mCanvas);

    Matrix expectedMatrix = new Matrix();
    expectedMatrix.setRotate(270, drawable.getBounds().centerX(), drawable.getBounds().centerY());
    assertFalse(drawable.mRotationMatrix.isIdentity());
    AndroidGraphicsTestUtils.assertEquals(expectedMatrix, drawable.mRotationMatrix);
    verifySetBounds(expectedMatrix);
  }

  private void verifySetBounds(Matrix rotationMatrix) {
    RectF expectedBounds = new RectF(mBounds);
    Matrix inverse = new Matrix();
    rotationMatrix.invert(inverse);
    inverse.mapRect(expectedBounds);
    verify(mDrawable).setBounds(
        (int) expectedBounds.left,
        (int) expectedBounds.top,
        (int) expectedBounds.right,
        (int) expectedBounds.bottom);
  }

}

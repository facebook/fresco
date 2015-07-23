/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.drawee.drawable;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link OrientedBitmapDrawable}
 */
@RunWith(RobolectricTestRunner.class)
public class OrientedBitmapDrawableTest {

  private Resources mResources;
  private Bitmap mBitmap;
  private Canvas mCanvas;

  @Before
  public void setUp() {
    mResources = Resources.getSystem();
    mBitmap = mock(Bitmap.class);
    mCanvas = mock(Canvas.class);
  }

  @Test
  public void testCreation_invalidAngle() {
    try {
      new OrientedBitmapDrawable(mResources, mBitmap, 20);
      fail();
    } catch (IllegalArgumentException e) {
      // Do nothing, expected.
    }
  }

  @Test
  public void testCreation_unknownAngle() {
    OrientedBitmapDrawable drawable = new OrientedBitmapDrawable(mResources, mBitmap, -1);
    drawable.draw(mCanvas);
    assertTrue(drawable.mRotationMatrix.isIdentity());
  }

  @Test
  public void testCreation_zeroDegrees() {
    OrientedBitmapDrawable drawable = new OrientedBitmapDrawable(mResources, mBitmap, 0);
    drawable.draw(mCanvas);
    assertTrue(drawable.mRotationMatrix.isIdentity());

  }

  @Test
  public void testCreation_nintyDegrees() {
    OrientedBitmapDrawable drawable = new OrientedBitmapDrawable(mResources, mBitmap, 90);
    drawable.draw(mCanvas);

    Matrix expectedMatrix = new Matrix();
    expectedMatrix.setRotate(90, drawable.getBounds().centerX(), drawable.getBounds().centerY());
    assertFalse(drawable.mRotationMatrix.isIdentity());
    AndroidGraphicsTestUtils.assertEquals(expectedMatrix, drawable.mRotationMatrix);
  }

  @Test
  public void testCreation_hundredAndEightyDegrees() {
    OrientedBitmapDrawable drawable = new OrientedBitmapDrawable(mResources, mBitmap, 180);
    drawable.draw(mCanvas);

    Matrix expectedMatrix = new Matrix();
    expectedMatrix.setRotate(180, drawable.getBounds().centerX(), drawable.getBounds().centerY());
    assertFalse(drawable.mRotationMatrix.isIdentity());
    AndroidGraphicsTestUtils.assertEquals(expectedMatrix, drawable.mRotationMatrix);
  }

  @Test
  public void testCreation_twoHundredAndSeventyDegrees() {
    OrientedBitmapDrawable drawable = new OrientedBitmapDrawable(mResources, mBitmap, 270);
    drawable.draw(mCanvas);

    Matrix expectedMatrix = new Matrix();
    expectedMatrix.setRotate(270, drawable.getBounds().centerX(), drawable.getBounds().centerY());
    assertFalse(drawable.mRotationMatrix.isIdentity());
    AndroidGraphicsTestUtils.assertEquals(expectedMatrix, drawable.mRotationMatrix);
  }

}

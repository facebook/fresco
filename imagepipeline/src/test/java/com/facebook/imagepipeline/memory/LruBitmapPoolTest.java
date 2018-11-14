/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.memory;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.spy;

import android.graphics.Bitmap;
import com.facebook.common.memory.MemoryTrimType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class LruBitmapPoolTest {

  private LruBitmapPool mPool;

  @Before
  public void setup() {
    mPool =
        spy(
            new LruBitmapPool(
                10 * 1024 * 1024, 1024 * 1024, NoOpPoolStatsTracker.getInstance(), null));
  }

  @Test
  public void testBitmapIsReused() {
    Bitmap expected = Bitmap.createBitmap(128, 128, Bitmap.Config.RGB_565);
    mPool.release(expected);

    Bitmap actual = mPool.get(128 * 128 * 2);

    assertSame(actual, expected);
  }

  @Test
  public void testBitmapWasTrimmed() {
    Bitmap expected = Bitmap.createBitmap(128, 128, Bitmap.Config.RGB_565);
    mPool.release(expected);

    assertEquals(1, ((LruBucketsPoolBackend)mPool.mStrategy).valueCount());

    mPool.trim(MemoryTrimType.OnAppBackgrounded);

    Bitmap actual = mPool.get(128 * 128 * 2);

    assertNotSame(actual, expected);
    assertEquals(0, ((LruBucketsPoolBackend)mPool.mStrategy).valueCount());
  }

  @Test
  public void testUniqueObjects() {
    Bitmap one = Bitmap.createBitmap(4, 4, Bitmap.Config.RGB_565);
    mPool.release(one);
    mPool.release(one);
    mPool.release(one);

    assertEquals(1, ((LruBucketsPoolBackend)mPool.mStrategy).valueCount());
  }
}

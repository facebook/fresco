/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.graphics.Bitmap;
import android.os.Build;
import org.junit.*;
import org.junit.runner.*;
import org.robolectric.*;

@RunWith(RobolectricTestRunner.class)
public class BitmapCounterTest {

  private static final int MAX_COUNT = 4;
  private static final int MAX_SIZE = MAX_COUNT + 1;

  private BitmapCounter mBitmapCounter;

  @Before
  public void setUp() {
    mBitmapCounter = new BitmapCounter(MAX_COUNT, MAX_SIZE);
  }

  @Test
  public void testBasic() {
    assertState(0, 0);
    assertTrue(mBitmapCounter.increase(bitmapForSize(1)));
    assertState(1, 1);
    assertTrue(mBitmapCounter.increase(bitmapForSize(2)));
    assertState(2, 3);
    mBitmapCounter.decrease(bitmapForSize(1));
    assertState(1, 2);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDecreaseTooMuch() {
    assertTrue(mBitmapCounter.increase(bitmapForSize(1)));
    mBitmapCounter.decrease(bitmapForSize(2));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDecreaseTooMany() {
    assertTrue(mBitmapCounter.increase(bitmapForSize(2)));
    mBitmapCounter.decrease(bitmapForSize(1));
    mBitmapCounter.decrease(bitmapForSize(1));
  }

  @Test
  public void testMaxSize() {
    assertTrue(mBitmapCounter.increase(bitmapForSize(MAX_SIZE)));
    assertState(1, MAX_SIZE);
  }

  @Test
  public void testMaxCount() {
    for (int i = 0; i < MAX_COUNT; ++i) {
      mBitmapCounter.increase(bitmapForSize(1));
    }
    assertState(MAX_COUNT, MAX_COUNT);
  }

  @Test()
  public void increaseTooBigObject() {
    assertFalse(mBitmapCounter.increase(bitmapForSize(MAX_SIZE + 1)));
    assertState(0, 0);
  }

  @Test()
  public void increaseTooManyObjects() {
    for (int i = 0; i < MAX_COUNT; ++i) {
      mBitmapCounter.increase(bitmapForSize(1));
    }
    assertFalse(mBitmapCounter.increase(bitmapForSize(1)));
    assertState(MAX_COUNT, MAX_COUNT);
  }

  private void assertState(int count, long size) {
    assertEquals(count, mBitmapCounter.getCount());
    assertEquals(size, mBitmapCounter.getSize());
  }

  private Bitmap bitmapForSize(int size) {
    final Bitmap bitmap = mock(Bitmap.class);
    doReturn(1).when(bitmap).getHeight();
    doReturn(size).when(bitmap).getRowBytes();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
      doReturn(size).when(bitmap).getByteCount();
    }
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
      doReturn(size).when(bitmap).getAllocationByteCount();
    }
    return bitmap;
  }
}

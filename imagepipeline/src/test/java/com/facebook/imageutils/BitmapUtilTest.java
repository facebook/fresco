/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.imageutils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

/**
 * Tests {@link BitmapUtil}
 */
@RunWith(RobolectricTestRunner.class)
@Config(emulateSdk = 18)
public class BitmapUtilTest {

  @Test
  public void testGetSizeInBytes() {
    // 0 for null
    assertEquals(0, BitmapUtil.getSizeInBytes(null));
    // 240 * 181 * 4 = 173760
    final Bitmap bitmap1 =
        BitmapFactory.decodeStream(BitmapUtilTest.class.getResourceAsStream("pngs/1.png"));
    assertEquals(173760, BitmapUtil.getSizeInBytes(bitmap1));
    // 240 * 246 * 4 = 236160
    final Bitmap bitmap2 =
        BitmapFactory.decodeStream(BitmapUtilTest.class.getResourceAsStream("pngs/2.png"));
    assertEquals(236160, BitmapUtil.getSizeInBytes(bitmap2));
    // 240 * 180 * 4 = 172800
    final Bitmap bitmap3 =
        BitmapFactory.decodeStream(BitmapUtilTest.class.getResourceAsStream("pngs/3.png"));
    assertEquals(172800, BitmapUtil.getSizeInBytes(bitmap3));
  }

}

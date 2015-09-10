/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.bitmaps;


import android.graphics.BitmapFactory;

import com.facebook.imagepipeline.memory.BitmapCounterProvider;
import com.facebook.imagepipeline.memory.FlexByteArrayPool;
import com.facebook.imagepipeline.nativecode.Bitmaps;

import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareOnlyThisForTest;

import org.robolectric.RobolectricTestRunner;

/**
 * Tests for {@link GingerbreadBitmapFactory}.
 */
@RunWith(RobolectricTestRunner.class)
@PrepareOnlyThisForTest({
    BitmapCounterProvider.class,
    BitmapFactory.class,
    Bitmaps.class})
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
public class GingerbreadBitmapFactoryTest extends DalvikBitmapFactoryTest{

  @Override
  protected DalvikBitmapFactory createDalvikBitmapFactoryImpl(
      FlexByteArrayPool flexByteArrayPool) {
    return new GingerbreadBitmapFactory(flexByteArrayPool);
  }
}

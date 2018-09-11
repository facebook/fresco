/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.bitmaps;

import android.graphics.Bitmap;
import com.facebook.common.references.ResourceReleaser;

/**
 * A releaser that just recycles (frees) bitmap memory immediately.
 */
public class SimpleBitmapReleaser implements ResourceReleaser<Bitmap> {

  private static SimpleBitmapReleaser sInstance;

  public static SimpleBitmapReleaser getInstance() {
    if (sInstance == null) {
      sInstance = new SimpleBitmapReleaser();
    }
    return sInstance;
  }

  private SimpleBitmapReleaser() {}

  @Override
  public void release(Bitmap value) {
    value.recycle();
  }
}

/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.image;

import android.graphics.Bitmap;

/**
 * {@link CloseableImage} that wraps a bitmap.
 */
public abstract class CloseableBitmap extends CloseableImage {

  /**
   * Gets the underlying bitmap.
   * Note: care must be taken because subclasses might be more sophisticated than that. For example,
   * animated bitmap may have many frames and this method will only return the first one.
   * @return the underlying bitmap
   */
  public abstract Bitmap getUnderlyingBitmap();

}

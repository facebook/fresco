/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.image;

import com.facebook.common.logging.FLog;
import java.io.Closeable;

/**
 * A simple wrapper around an image that implements {@link Closeable}
 */
public abstract class CloseableImage implements Closeable, ImageInfo {
  private static final String TAG = "CloseableImage";

  /**
   * @return size in bytes of the bitmap(s)
   */
  public abstract int getSizeInBytes();

  /**
   * Closes this instance and releases the resources.
   */
  @Override
  public abstract void close();

  /**
   * Returns whether this instance is closed.
   */
  public abstract boolean isClosed();

  /**
   * Returns quality information for the image.
   * <p> Image classes that can contain intermediate results should override this as appropriate.
   */
  @Override
  public QualityInfo getQualityInfo() {
    return ImmutableQualityInfo.FULL_QUALITY;
  }

  /**
   * Whether or not this image contains state for a particular view of the image (for example,
   * the image for an animated GIF might contain the current frame being viewed). This means
   * that the image should not be stored in the bitmap cache.
   */
  public boolean isStateful() {
    return false;
  }

  /**
   * Ensures that the underlying resources are always properly released.
   */
  @Override
  protected void finalize() throws Throwable {
    if (isClosed()) {
      return;
    }
    FLog.w(
        TAG,
        "finalize: %s %x still open.",
        this.getClass().getSimpleName(),
        System.identityHashCode(this));
    try {
      close();
    } finally {
      super.finalize();
    }
  }
}

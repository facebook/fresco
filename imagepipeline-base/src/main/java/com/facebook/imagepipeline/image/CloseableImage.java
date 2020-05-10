/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.image;

import com.facebook.common.logging.FLog;
import java.io.Closeable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;

/** A simple wrapper around an image that implements {@link Closeable} */
public abstract class CloseableImage implements Closeable, ImageInfo, HasImageMetadata {
  private static final String TAG = "CloseableImage";
  private OriginalEncodedImageInfo mOriginalEncodedImageInfo;

  /** @return size in bytes of the bitmap(s) */
  public abstract int getSizeInBytes();

  /** Closes this instance and releases the resources. */
  @Override
  public abstract void close();

  /** Returns whether this instance is closed. */
  public abstract boolean isClosed();

  /**
   * Returns quality information for the image.
   *
   * <p>Image classes that can contain intermediate results should override this as appropriate.
   */
  @Override
  public QualityInfo getQualityInfo() {
    return ImmutableQualityInfo.FULL_QUALITY;
  }

  /**
   * Whether or not this image contains state for a particular view of the image (for example, the
   * image for an animated GIF might contain the current frame being viewed). This means that the
   * image should not be stored in the bitmap cache.
   */
  public boolean isStateful() {
    return false;
  }

  public OriginalEncodedImageInfo getOriginalEncodedImageInfo() {
    return mOriginalEncodedImageInfo;
  }

  @Override
  public @Nonnull Map<String, Object> getAsExtras() {
    if (mOriginalEncodedImageInfo == null) {
      return Collections.emptyMap();
    }

    HashMap<String, Object> extras = new HashMap<>();
    extras.put("encoded_width", mOriginalEncodedImageInfo.getWidth());
    extras.put("encoded_height", mOriginalEncodedImageInfo.getHeight());
    extras.put("encoded_size", mOriginalEncodedImageInfo.getSize());
    return extras;
  }

  public void setOriginalEncodedImageInfo(OriginalEncodedImageInfo originalEncodedImageInfo) {
    mOriginalEncodedImageInfo = originalEncodedImageInfo;
  }

  /** Ensures that the underlying resources are always properly released. */
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

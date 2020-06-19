/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.image;

import com.facebook.common.logging.FLog;
import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;

/** A simple wrapper around an image that implements {@link Closeable} */
public abstract class CloseableImage implements Closeable, ImageInfo, HasImageMetadata {
  private static final String TAG = "CloseableImage";
  private Map<String, Object> mExtras = new HashMap<>();
  /* Extras we want to set to the image */
  private static final String[] mImageExtrasList =
      new String[] {
        "encoded_size",
        "encoded_width",
        "encoded_height",
        "uri_source",
        "image_format",
        "bitmap_config"
      };

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

  @Override
  public @Nonnull Map<String, Object> getExtras() {
    return mExtras;
  }

  /** Sets extras that match mImageExtrasList to this image from supplied extras */
  public void setImageExtras(Map<String, Object> extras) {
    if (extras == null) return;

    for (String extra : mImageExtrasList) {
      Object val = extras.get(extra);
      if (val == null) continue;
      mExtras.put(extra, val);
    }
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

/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.image;

import com.facebook.infer.annotation.Nullsafe;
import java.io.Closeable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/** A simple wrapper around an image that implements {@link Closeable} */
@Nullsafe(Nullsafe.Mode.LOCAL)
public abstract class BaseCloseableImage implements CloseableImage {
  public boolean sCreateImageInfo;

  private Map<String, Object> mExtras = new HashMap<>();

  /* Extras we want to set to the image */
  private static final Set<String> mImageExtrasList =
      new HashSet<>(
          Arrays.asList(
              "encoded_size",
              "encoded_width",
              "encoded_height",
              "uri_source",
              "image_format",
              "bitmap_config",
              "is_rounded",
              "non_fatal_decode_error"));

  private @Nullable MutableImageInfo mCacheImageInfo;

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
  @Override
  public boolean isStateful() {
    return false;
  }

  @Override
  public Map<String, Object> getExtras() {
    return mExtras;
  }

  /** Sets extras that match mImageExtrasList to this image from supplied extras */
  @Override
  public void setImageExtras(@Nullable Map<String, Object> extras) {
    if (extras == null) {
      return;
    }

    for (String extra : mImageExtrasList) {
      Object val = extras.get(extra);
      if (val == null) {
        continue;
      }
      mExtras.put(extra, val);
    }
  }

  @Override
  public void setImageExtra(String extra, Object value) {
    if (mImageExtrasList.contains(extra)) {
      mExtras.put(extra, value);
    }
  }

  @Override
  public ImageInfo getImageInfo() {
    if (!sCreateImageInfo) {
      return this;
    }

    if (mCacheImageInfo == null) {
      mCacheImageInfo =
          new MutableImageInfo(
              getWidth(), getHeight(), getSizeInBytes(), getQualityInfo(), getExtras());
    } else {
      mCacheImageInfo.width = getWidth();
      mCacheImageInfo.height = getHeight();
      mCacheImageInfo.qualityInfo = getQualityInfo();
      mCacheImageInfo.extras = getExtras();
      mCacheImageInfo.sizeInBytes = getSizeInBytes();
    }
    return mCacheImageInfo;
  }
}

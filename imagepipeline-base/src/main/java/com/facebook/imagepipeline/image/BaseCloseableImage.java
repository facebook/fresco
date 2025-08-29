/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.image;

import com.facebook.fresco.middleware.HasExtraData;
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

  private final Map<String, Object> mExtras = new HashMap<>();

  /* Extras we want to set to the image */
  private static final Set<String> mImageExtrasList =
      new HashSet<>(
          Arrays.asList(
              HasExtraData.KEY_ENCODED_SIZE,
              HasExtraData.KEY_ENCODED_WIDTH,
              HasExtraData.KEY_ENCODED_HEIGHT,
              HasExtraData.KEY_URI_SOURCE,
              HasExtraData.KEY_IMAGE_FORMAT,
              HasExtraData.KEY_BITMAP_CONFIG,
              HasExtraData.KEY_IS_ROUNDED,
              HasExtraData.KEY_NON_FATAL_DECODE_ERROR,
              HasExtraData.KEY_SF_ORIGINAL_URL,
              HasExtraData.KEY_SF_FETCH_STRATEGY,
              HasExtraData.KEY_COLOR_SPACE,
              HasExtraData.KEY_SF_VARIATION,
              HasExtraData.KEY_SF_QUERY));

  private @Nullable ImageInfo mCacheImageInfo;

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
  public void putExtras(@Nullable Map<String, ? extends Object> extras) {
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
  public <E> void putExtra(String extra, @Nullable E value) {
    if (mImageExtrasList.contains(extra)) {
      mExtras.put(extra, value);
    }
  }

  @Override
  public <T> T getExtra(String key) {
    return getExtra(key, null);
  }

  @Override
  public <T> T getExtra(String key, @Nullable T valueIfNotFound) {
    Object value = mExtras.get(key);
    if (value == null) {
      return valueIfNotFound;
    }
    //noinspection unchecked
    return (T) value;
  }

  @Override
  public ImageInfo getImageInfo() {
    if (mCacheImageInfo == null) {
      mCacheImageInfo =
          new ImageInfoImpl(
              getWidth(), getHeight(), getSizeInBytes(), getQualityInfo(), getExtras());
    }
    return mCacheImageInfo;
  }
}

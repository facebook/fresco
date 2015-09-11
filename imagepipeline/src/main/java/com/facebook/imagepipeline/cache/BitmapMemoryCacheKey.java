/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.cache;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import java.util.Locale;

import com.facebook.cache.common.CacheKey;
import com.facebook.common.internal.Objects;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.util.HashCodeUtil;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.common.ResizeOptions;

/**
 * Cache key for BitmapMemoryCache
 */
@Immutable
public class BitmapMemoryCacheKey implements CacheKey {

  private final String mSourceString;
  private final @Nullable ResizeOptions mResizeOptions;
  private final boolean mAutoRotated;
  private final ImageDecodeOptions mImageDecodeOptions;
  private final @Nullable CacheKey mPostprocessorCacheKey;
  private final @Nullable String mPostprocessorName;
  private final int mHash;

  public BitmapMemoryCacheKey(
      String sourceString,
      @Nullable ResizeOptions resizeOptions,
      boolean autoRotated,
      ImageDecodeOptions imageDecodeOptions,
      @Nullable CacheKey postprocessorCacheKey,
      @Nullable String postprocessorName) {
    mSourceString = Preconditions.checkNotNull(sourceString);
    mResizeOptions = resizeOptions;
    mAutoRotated = autoRotated;
    mImageDecodeOptions = imageDecodeOptions;
    mPostprocessorCacheKey = postprocessorCacheKey;
    mPostprocessorName = postprocessorName;
    mHash = HashCodeUtil.hashCode(
        sourceString.hashCode(),
        (resizeOptions != null) ? resizeOptions.hashCode() : 0,
        autoRotated ? Boolean.TRUE.hashCode() : Boolean.FALSE.hashCode(),
        mImageDecodeOptions,
        mPostprocessorCacheKey,
        postprocessorName);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof BitmapMemoryCacheKey)) {
      return false;
    }
    BitmapMemoryCacheKey otherKey = (BitmapMemoryCacheKey) o;
    return mHash == otherKey.mHash &&
        mSourceString.equals(otherKey.mSourceString) &&
        Objects.equal(this.mResizeOptions, otherKey.mResizeOptions) &&
        mAutoRotated == otherKey.mAutoRotated &&
        Objects.equal(mImageDecodeOptions, otherKey.mImageDecodeOptions) &&
        Objects.equal(mPostprocessorCacheKey, otherKey.mPostprocessorCacheKey) &&
        Objects.equal(mPostprocessorName, otherKey.mPostprocessorName);
  }

  @Override
  public int hashCode() {
    return mHash;
  }

  public String getSourceUriString() {
    return mSourceString;
  }

  @Nullable
  public String getPostprocessorName() {
    return mPostprocessorName;
  }

  @Override
  public String toString() {
    return String.format(
        (Locale) null,
        "%s_%s_%s_%s_%s_%s_%d",
        mSourceString,
        mResizeOptions,
        Boolean.toString(mAutoRotated),
        mImageDecodeOptions,
        mPostprocessorCacheKey,
        mPostprocessorName,
        mHash);
  }
}

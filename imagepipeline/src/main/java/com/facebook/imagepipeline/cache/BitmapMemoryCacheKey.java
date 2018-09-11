/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache;

import android.net.Uri;
import com.facebook.cache.common.CacheKey;
import com.facebook.common.internal.Objects;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.time.RealtimeSinceBootClock;
import com.facebook.common.util.HashCodeUtil;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.common.RotationOptions;
import java.util.Locale;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Cache key for BitmapMemoryCache
 */
@Immutable
public class BitmapMemoryCacheKey implements CacheKey {

  private final String mSourceString;
  private final @Nullable ResizeOptions mResizeOptions;
  private final RotationOptions mRotationOptions;
  private final ImageDecodeOptions mImageDecodeOptions;
  private final @Nullable CacheKey mPostprocessorCacheKey;
  private final @Nullable String mPostprocessorName;
  private final int mHash;
  private final Object mCallerContext;
  private final long mCacheTime;

  public BitmapMemoryCacheKey(
      String sourceString,
      @Nullable ResizeOptions resizeOptions,
      RotationOptions rotationOptions,
      ImageDecodeOptions imageDecodeOptions,
      @Nullable CacheKey postprocessorCacheKey,
      @Nullable String postprocessorName,
      Object callerContext) {
    mSourceString = Preconditions.checkNotNull(sourceString);
    mResizeOptions = resizeOptions;
    mRotationOptions = rotationOptions;
    mImageDecodeOptions = imageDecodeOptions;
    mPostprocessorCacheKey = postprocessorCacheKey;
    mPostprocessorName = postprocessorName;
    mHash = HashCodeUtil.hashCode(
        sourceString.hashCode(),
        (resizeOptions != null) ? resizeOptions.hashCode() : 0,
        rotationOptions.hashCode(),
        mImageDecodeOptions,
        mPostprocessorCacheKey,
        postprocessorName);
    mCallerContext = callerContext;
    mCacheTime = RealtimeSinceBootClock.get().now();
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
        Objects.equal(this.mRotationOptions, otherKey.mRotationOptions) &&
        Objects.equal(mImageDecodeOptions, otherKey.mImageDecodeOptions) &&
        Objects.equal(mPostprocessorCacheKey, otherKey.mPostprocessorCacheKey) &&
        Objects.equal(mPostprocessorName, otherKey.mPostprocessorName);
  }

  @Override
  public int hashCode() {
    return mHash;
  }

  @Override
  public boolean containsUri(Uri uri) {
    return getUriString().contains(uri.toString());
  }

  @Override
  public String getUriString() {
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
        mRotationOptions,
        mImageDecodeOptions,
        mPostprocessorCacheKey,
        mPostprocessorName,
        mHash);
  }

  public Object getCallerContext() {
    return mCallerContext;
  }

  public long getInBitmapCacheSince() {
    return mCacheTime;
  }
}

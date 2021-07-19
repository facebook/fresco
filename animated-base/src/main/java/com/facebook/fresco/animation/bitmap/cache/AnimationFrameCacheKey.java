/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.bitmap.cache;

import android.net.Uri;
import com.facebook.cache.common.CacheKey;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

/* Frame cache key for animation */
@Nullsafe(Nullsafe.Mode.STRICT)
public class AnimationFrameCacheKey implements CacheKey {

  private static final String URI_PREFIX = "anim://";

  private final String mAnimationUriString;

  private final boolean mDeepEquals;

  public AnimationFrameCacheKey(int imageId) {
    this(imageId, false);
  }

  public AnimationFrameCacheKey(int imageId, boolean deepEquals) {
    mAnimationUriString = URI_PREFIX + imageId;
    mDeepEquals = deepEquals;
  }

  @Override
  public boolean containsUri(Uri uri) {
    return uri.toString().startsWith(mAnimationUriString);
  }

  @Override
  public String getUriString() {
    return mAnimationUriString;
  }

  @Override
  public boolean isResourceIdForDebugging() {
    return false;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (!mDeepEquals) {
      return super.equals(o);
    }

    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    AnimationFrameCacheKey that = (AnimationFrameCacheKey) o;
    return mAnimationUriString.equals(that.mAnimationUriString);
  }

  @Override
  public int hashCode() {
    if (!mDeepEquals) {
      return super.hashCode();
    }
    return mAnimationUriString.hashCode();
  }
}

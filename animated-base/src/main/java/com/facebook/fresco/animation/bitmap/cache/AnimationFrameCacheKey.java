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

/* Frame cache key for animation */
@Nullsafe(Nullsafe.Mode.STRICT)
public class AnimationFrameCacheKey implements CacheKey {

  private static final String URI_PREFIX = "anim://";

  private final String mAnimationUriString;

  public AnimationFrameCacheKey(int imageId) {
    mAnimationUriString = URI_PREFIX + imageId;
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
}

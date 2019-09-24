/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.fresco.animation.bitmap.cache;

import android.net.Uri;
import com.facebook.cache.common.CacheKey;

/* Frame cache key for animation */
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
}

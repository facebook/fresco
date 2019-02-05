/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.cache.common;

import android.net.Uri;
import javax.annotation.Nullable;

/**
 * Extension of {@link SimpleCacheKey} which adds the ability to hold a caller context. This can be
 * of use for debugging and has no bearing on equality.
 */
public class DebuggingCacheKey extends SimpleCacheKey {

  private final @Nullable Object mCallerContext;
  private final Uri mSourceUri;

  public DebuggingCacheKey(String key, @Nullable Object callerContext, Uri sourceUri) {
    super(key);
    mCallerContext = callerContext;
    mSourceUri = sourceUri;
  }

  @Nullable
  public Object getCallerContext() {
    return mCallerContext;
  }

  /**
   * Original URI the image was fetched from.
   */
  public Uri getSourceUri() {
    return mSourceUri;
  }
}

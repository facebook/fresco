/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.cache.common;

import javax.annotation.Nullable;

/**
 * Extension of {@link SimpleCacheKey} which adds the ability to hold a caller context. This can be
 * of use for debugging and has no bearing on equality.
 */
public class DebuggingCacheKey extends SimpleCacheKey {

  private final Object mCallerContext;

  public DebuggingCacheKey(String key, @Nullable Object callerContext) {
    super(key);
    mCallerContext = callerContext;
  }

  @Nullable
  public Object getCallerContext() {
    return mCallerContext;
  }
}

/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.cache.common;

import javax.annotation.Nullable;

/**
 * An implementation of {@link CacheErrorLogger} that doesn't do anything.
 */
public class NoOpCacheErrorLogger implements CacheErrorLogger {
  private static @Nullable NoOpCacheErrorLogger sInstance = null;

  private NoOpCacheErrorLogger() {
  }

  public static synchronized NoOpCacheErrorLogger getInstance() {
    if (sInstance == null) {
      sInstance = new NoOpCacheErrorLogger();
    }
    return sInstance;
  }

  /**
   * Log an error of the specified category.
   * @param category Error category
   * @param clazz Class reporting the error
   * @param message An optional error message
   * @param throwable An optional exception
   */
  @Override
  public void logError(
      CacheErrorCategory category,
      Class<?> clazz,
      String message,
      @Nullable Throwable throwable) {
  }
}

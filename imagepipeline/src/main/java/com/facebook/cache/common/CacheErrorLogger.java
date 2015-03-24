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
 * An interface for logging various cache errors.
 */
public interface CacheErrorLogger {

  /**
   * A categorizaton of different cache and storage related errors.
   */
  public enum CacheErrorCategory {
    READ_DECODE,
    READ_FILE,
    READ_FILE_NOT_FOUND,
    READ_INVALID_ENTRY,

    WRITE_ENCODE,
    WRITE_CREATE_TEMPFILE,
    WRITE_UPDATE_FILE_NOT_FOUND,
    WRITE_RENAME_FILE_TEMPFILE_NOT_FOUND,
    WRITE_RENAME_FILE_TEMPFILE_PARENT_NOT_FOUND,
    WRITE_RENAME_FILE_OTHER,
    WRITE_CREATE_DIR,
    WRITE_CALLBACK_ERROR,
    WRITE_INVALID_ENTRY,

    DELETE_FILE,

    EVICTION,
    GENERIC_IO,
    OTHER
  }

  /**
   * Log an error of the specified category.
   * @param category Error category
   * @param clazz Class reporting the error
   * @param message An optional error message
   * @param throwable An optional exception
   */
  public void logError(
      CacheErrorCategory category,
      Class<?> clazz,
      String message,
      @Nullable Throwable throwable);
}

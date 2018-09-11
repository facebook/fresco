/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
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
  enum CacheErrorCategory {
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
  void logError(
      CacheErrorCategory category,
      Class<?> clazz,
      String message,
      @Nullable Throwable throwable);
}

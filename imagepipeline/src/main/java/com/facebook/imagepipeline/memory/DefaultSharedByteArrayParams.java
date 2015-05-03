/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.memory;

import com.facebook.common.util.ByteConstants;

/**
 * Provides pool parameters ({@link PoolParams}) for {@link SharedByteArray}
 */
public class DefaultSharedByteArrayParams {
  // the default max buffer size we'll use
  private static final int DEFAULT_MAX_BYTE_ARRAY_SIZE = 4 * ByteConstants.MB;
  // the min buffer size we'll use
  private static final int DEFAULT_MIN_BYTE_ARRAY_SIZE = 128 * ByteConstants.KB;

  private DefaultSharedByteArrayParams() {
  }

  public static PoolParams get() {
    return new PoolParams(
        DEFAULT_MAX_BYTE_ARRAY_SIZE,
        DEFAULT_MAX_BYTE_ARRAY_SIZE,
        null,
        DEFAULT_MIN_BYTE_ARRAY_SIZE,
        DEFAULT_MAX_BYTE_ARRAY_SIZE
    );
  }
}

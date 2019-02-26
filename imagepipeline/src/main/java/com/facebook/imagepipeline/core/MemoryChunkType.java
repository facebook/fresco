/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.core;

import static com.facebook.imagepipeline.core.MemoryChunkType.BUFFER_MEMORY;
import static com.facebook.imagepipeline.core.MemoryChunkType.NATIVE_MEMORY;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import androidx.annotation.IntDef;
import java.lang.annotation.Retention;

/** Memory type that indicates which kind of memory implementation will be used. */
@Retention(SOURCE)
@IntDef({
  NATIVE_MEMORY,
  BUFFER_MEMORY,
})
public @interface MemoryChunkType {
  int NATIVE_MEMORY = 0;
  int BUFFER_MEMORY = 1;
}

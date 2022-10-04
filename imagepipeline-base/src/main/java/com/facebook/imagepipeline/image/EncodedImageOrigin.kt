/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.image

enum class EncodedImageOrigin(private val origin: String) {
  NOT_SET("not_set"),
  NETWORK("network"),
  DISK("disk"),
  ENCODED_MEM_CACHE("encoded_mem_cache");

  override fun toString(): String = origin
}

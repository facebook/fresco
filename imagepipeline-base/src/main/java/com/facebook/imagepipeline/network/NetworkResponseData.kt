/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.network

import java.io.InputStream

sealed class NetworkResponseData {
  /** Network response backed by a [ByteArray]. [length] is the number of valid bytes in [data]. */
  class Bytes(val data: ByteArray, val length: Int) : NetworkResponseData()

  /** Network response backed by an [InputStream]. Use [length] = -1 if the size is unknown. */
  class Stream(val stream: InputStream, val length: Int) : NetworkResponseData()
}

/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.image

import android.net.Uri
import kotlin.jvm.JvmField

class OriginalEncodedImageInfo {

  val uri: Uri?
  val origin: EncodedImageOrigin?
  val callerContext: Any?
  val width: Int
  val height: Int
  val size: Int

  private constructor() {
    uri = null
    origin = EncodedImageOrigin.NOT_SET
    callerContext = null
    width = -1
    height = -1
    size = -1
  }

  constructor(
      sourceUri: Uri?,
      origin: EncodedImageOrigin?,
      callerContext: Any?,
      width: Int,
      height: Int,
      size: Int
  ) {
    uri = sourceUri
    this.origin = origin
    this.callerContext = callerContext
    this.width = width
    this.height = height
    this.size = size
  }

  companion object {
    @JvmField val EMPTY = OriginalEncodedImageInfo()
  }
}

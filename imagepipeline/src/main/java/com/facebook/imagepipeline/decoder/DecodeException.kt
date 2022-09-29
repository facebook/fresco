/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.decoder

import com.facebook.imagepipeline.image.EncodedImage

class DecodeException : RuntimeException {

  val encodedImage: EncodedImage

  constructor(message: String?, encodedImage: EncodedImage) : super(message) {
    this.encodedImage = encodedImage
  }

  constructor(message: String?, t: Throwable?, encodedImage: EncodedImage) : super(message, t) {
    this.encodedImage = encodedImage
  }
}

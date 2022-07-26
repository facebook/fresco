/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.decoder

import com.facebook.imagepipeline.common.ImageDecodeOptions
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.image.EncodedImage
import com.facebook.imagepipeline.image.QualityInfo

/** Image decoder interface. Takes an [EncodedImage] and creates a [CloseableImage]. */
fun interface ImageDecoder {
  fun decode(
      encodedImage: EncodedImage,
      length: Int,
      qualityInfo: QualityInfo,
      options: ImageDecodeOptions
  ): CloseableImage?
}

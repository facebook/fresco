/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.animated.factory

import android.graphics.Bitmap
import com.facebook.imagepipeline.common.ImageDecodeOptions
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.image.EncodedImage

/** Decoder for animated images. */
interface AnimatedImageFactory {

  /**
   * Decodes a GIF into a CloseableImage.
   *
   * @param encodedImage encoded image (native byte array holding the encoded bytes and meta data)
   * @param options the options for the decode
   * @param bitmapConfig the Bitmap.Config used to generate the output bitmaps
   * @return a [CloseableImage] for the GIF image
   */
  fun decodeGif(
      encodedImage: EncodedImage,
      options: ImageDecodeOptions,
      bitmapConfig: Bitmap.Config,
  ): CloseableImage

  /**
   * Decode a WebP into a CloseableImage.
   *
   * @param encodedImage encoded image (native byte array holding the encoded bytes and meta data)
   * @param options the options for the decode
   * @param bitmapConfig the Bitmap.Config used to generate the output bitmaps
   * @return a [CloseableImage] for the WebP image
   */
  fun decodeWebP(
      encodedImage: EncodedImage,
      options: ImageDecodeOptions,
      bitmapConfig: Bitmap.Config,
  ): CloseableImage
}

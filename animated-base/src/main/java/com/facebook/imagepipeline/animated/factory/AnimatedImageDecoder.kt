/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.animated.factory

import com.facebook.imagepipeline.animated.base.AnimatedImage
import com.facebook.imagepipeline.common.ImageDecodeOptions
import java.nio.ByteBuffer

interface AnimatedImageDecoder {

  /**
   * Factory method to create the AnimatedImage from a native pointer
   *
   * @param nativePtr The native pointer
   * @param sizeInBytes The size in byte to allocate
   * @param options The options for decoding
   * @return The AnimatedImage allocation
   */
  fun decodeFromNativeMemory(
      nativePtr: Long,
      sizeInBytes: Int,
      options: ImageDecodeOptions
  ): AnimatedImage?

  /**
   * Factory method to create the AnimatedImage from a ByteBuffer
   *
   * @param byteBuffer The ByteBuffer containing the image
   * @param options The options for decoding
   * @return The AnimatedImage allocation
   */
  fun decodeFromByteBuffer(byteBuffer: ByteBuffer, options: ImageDecodeOptions): AnimatedImage?
}

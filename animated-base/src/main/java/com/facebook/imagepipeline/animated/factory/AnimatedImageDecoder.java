/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.animated.factory;

import com.facebook.imagepipeline.animated.base.AnimatedImage;
import java.nio.ByteBuffer;

public interface AnimatedImageDecoder {

  /**
   * Factory method to create the AnimatedImage from the
   * @param nativePtr The native pointer
   * @param sizeInBytes The size in byte to allocate
   * @return The AnimatedImage allocation
   */
  AnimatedImage decode(long nativePtr, int sizeInBytes);

  /**
   * Factory method to create the AnimatedImage from a ByteBuffer
   *
   * @param byteBuffer The ByteBuffer containing the image
   * @return The AnimatedImage allocation
   */
  AnimatedImage decode(ByteBuffer byteBuffer);
}

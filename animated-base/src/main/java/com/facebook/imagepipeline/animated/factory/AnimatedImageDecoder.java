/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.animated.factory;

import com.facebook.imagepipeline.animated.base.AnimatedImage;

public interface AnimatedImageDecoder {

  /**
   * Factory method to create the AnimatedImage from the
   * @param nativePtr The native pointer
   * @param sizeInBytes The size in byte to allocate
   * @return The AnimatedImage allocation
   */
  AnimatedImage decode(long nativePtr, int sizeInBytes);
}

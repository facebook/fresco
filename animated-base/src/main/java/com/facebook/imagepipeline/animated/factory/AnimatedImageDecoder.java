/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
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

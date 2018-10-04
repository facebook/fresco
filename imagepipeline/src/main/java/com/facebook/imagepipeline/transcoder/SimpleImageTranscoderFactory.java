/*
 * Copyright (c) 2018-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.transcoder;

import com.facebook.imageformat.ImageFormat;

/** Factory class to create an {@link SimpleImageTranscoder} */
public class SimpleImageTranscoderFactory implements ImageTranscoderFactory {

  private final int mMaxBitmapSize;

  public SimpleImageTranscoderFactory(final int maxBitmapSize) {
    mMaxBitmapSize = maxBitmapSize;
  }

  @Override
  public ImageTranscoder createImageTranscoder(ImageFormat imageFormat, boolean isResizingEnabled) {
    return new SimpleImageTranscoder(isResizingEnabled, mMaxBitmapSize);
  }
}

/*
 * Copyright (c) 2018-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.transcoder;

import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;
import com.facebook.imagepipeline.platform.PlatformDecoder;

/** Factory class to create an {@link SimpleImageTranscoder} */
public class SimpleImageTranscoderFactory implements ImageTranscoderFactory {

  private final int mMaxBitmapSize;
  private final PlatformDecoder mPlatformDecoder;
  private final PlatformBitmapFactory mPlatformBitmapFactory;

  public SimpleImageTranscoderFactory(
      final int maxBitmapSize,
      final PlatformDecoder platformDecoder,
      final PlatformBitmapFactory platformBitmapFactory) {
    mMaxBitmapSize = maxBitmapSize;
    mPlatformDecoder = platformDecoder;
    mPlatformBitmapFactory = platformBitmapFactory;
  }

  @Override
  public ImageTranscoder createImageTranscoder(boolean isResizingEnabled) {
    return new SimpleImageTranscoder(
        isResizingEnabled, mMaxBitmapSize, mPlatformDecoder, mPlatformBitmapFactory);
  }
}

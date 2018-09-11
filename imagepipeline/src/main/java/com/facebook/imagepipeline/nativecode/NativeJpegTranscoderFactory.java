/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.nativecode;

import com.facebook.imagepipeline.transcoder.ImageTranscoder;
import com.facebook.imagepipeline.transcoder.ImageTranscoderFactory;

public class NativeJpegTranscoderFactory implements ImageTranscoderFactory {

  private final int mMaxBitmapSize;
  private final boolean mUseDownSamplingRatio;

  public NativeJpegTranscoderFactory(final int maxBitmapSize, final boolean useDownSamplingRatio) {
    mMaxBitmapSize = maxBitmapSize;
    mUseDownSamplingRatio = useDownSamplingRatio;
  }

  @Override
  public ImageTranscoder createImageTranscoder(boolean isResizingEnabled) {
    return new NativeJpegTranscoder(isResizingEnabled, mMaxBitmapSize, mUseDownSamplingRatio);
  }
}

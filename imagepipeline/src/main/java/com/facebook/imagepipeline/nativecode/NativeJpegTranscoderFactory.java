/*
 * Copyright (c) 2017-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.nativecode;

import com.facebook.imagepipeline.transcoder.ImageTranscoder;
import com.facebook.imagepipeline.transcoder.ImageTranscoderFactory;

public class NativeJpegTranscoderFactory implements ImageTranscoderFactory {

  private ImageTranscoder mImageTranscoder;

  private final boolean mResizingEnabled;
  private final int mMaxBitmapSize;
  private final boolean mUseDownSamplingRatio;

  public NativeJpegTranscoderFactory(
      final boolean resizingEnabled, final int maxBitmapSize, final boolean useDownSamplingRatio) {
    mResizingEnabled = resizingEnabled;
    mMaxBitmapSize = maxBitmapSize;
    mUseDownSamplingRatio = useDownSamplingRatio;
  }

  @Override
  public synchronized ImageTranscoder createImageTranscoder() {
    if (mImageTranscoder == null) {
      mImageTranscoder =
          new NativeJpegTranscoder(mResizingEnabled, mMaxBitmapSize, mUseDownSamplingRatio);
    }
    return mImageTranscoder;
  }
}

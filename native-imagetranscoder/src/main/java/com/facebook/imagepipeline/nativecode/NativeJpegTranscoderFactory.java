/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.nativecode;

import com.facebook.common.internal.DoNotStrip;
import com.facebook.imageformat.DefaultImageFormats;
import com.facebook.imageformat.ImageFormat;
import com.facebook.imagepipeline.transcoder.ImageTranscoder;
import com.facebook.imagepipeline.transcoder.ImageTranscoderFactory;
import javax.annotation.Nullable;

@DoNotStrip
public class NativeJpegTranscoderFactory implements ImageTranscoderFactory {

  private final int mMaxBitmapSize;
  private final boolean mUseDownSamplingRatio;

  @DoNotStrip
  public NativeJpegTranscoderFactory(final int maxBitmapSize, final boolean useDownSamplingRatio) {
    mMaxBitmapSize = maxBitmapSize;
    mUseDownSamplingRatio = useDownSamplingRatio;
  }

  @DoNotStrip
  @Override
  @Nullable
  public ImageTranscoder createImageTranscoder(ImageFormat imageFormat, boolean isResizingEnabled) {
    if (imageFormat != DefaultImageFormats.JPEG) {
      return null;
    }
    return new NativeJpegTranscoder(isResizingEnabled, mMaxBitmapSize, mUseDownSamplingRatio);
  }
}

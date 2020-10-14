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
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.STRICT)
@DoNotStrip
public class NativeJpegTranscoderFactory implements ImageTranscoderFactory {

  private final int mMaxBitmapSize;
  private final boolean mUseDownSamplingRatio;
  private final boolean mEnsureTranscoderLibraryLoaded;

  @DoNotStrip
  public NativeJpegTranscoderFactory(
      final int maxBitmapSize,
      final boolean useDownSamplingRatio,
      final boolean ensureTranscoderLibraryLoaded) {
    mMaxBitmapSize = maxBitmapSize;
    mUseDownSamplingRatio = useDownSamplingRatio;
    mEnsureTranscoderLibraryLoaded = ensureTranscoderLibraryLoaded;
  }

  @DoNotStrip
  @Override
  @Nullable
  public ImageTranscoder createImageTranscoder(ImageFormat imageFormat, boolean isResizingEnabled) {
    if (imageFormat != DefaultImageFormats.JPEG) {
      return null;
    }
    return new NativeJpegTranscoder(
        isResizingEnabled, mMaxBitmapSize, mUseDownSamplingRatio, mEnsureTranscoderLibraryLoaded);
  }
}

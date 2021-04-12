/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.nativecode;

import com.facebook.common.internal.Supplier;
import com.facebook.fresco.vito.core.impl.ImagePipelineUtilsImpl;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class NativeCircularBitmapRounding implements ImagePipelineUtilsImpl.CircularBitmapRounding {

  private @Nullable ImageDecodeOptions mCircularImageDecodeOptions;
  private @Nullable ImageDecodeOptions mCircularImageDecodeOptionsAntiAliased;

  private final Supplier<Boolean> mUseFastNativeRounding;

  public NativeCircularBitmapRounding(Supplier<Boolean> useFastNativeRounding) {
    mUseFastNativeRounding = useFastNativeRounding;
  }

  @Override
  public ImageDecodeOptions getDecodeOptions(boolean antiAliased) {
    if (antiAliased) {
      if (mCircularImageDecodeOptionsAntiAliased == null) {
        mCircularImageDecodeOptionsAntiAliased =
            ImageDecodeOptions.newBuilder()
                .setBitmapTransformation(
                    new CircularBitmapTransformation(true, mUseFastNativeRounding.get()))
                .build();
      }
      return mCircularImageDecodeOptionsAntiAliased;
    } else {
      if (mCircularImageDecodeOptions == null) {
        mCircularImageDecodeOptions =
            ImageDecodeOptions.newBuilder()
                .setBitmapTransformation(
                    new CircularBitmapTransformation(false, mUseFastNativeRounding.get()))
                .build();
      }
      return mCircularImageDecodeOptions;
    }
  }
}

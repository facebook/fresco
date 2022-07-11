/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.nativecode

import com.facebook.common.internal.Supplier
import com.facebook.fresco.vito.core.impl.ImagePipelineUtilsImpl.CircularBitmapRounding
import com.facebook.imagepipeline.common.ImageDecodeOptions

class NativeCircularBitmapRounding(private val useFastNativeRounding: Supplier<Boolean>) :
    CircularBitmapRounding {
  private val circularImageDecodeOptions: ImageDecodeOptions by lazy {
    ImageDecodeOptions.newBuilder()
        .setBitmapTransformation(CircularBitmapTransformation(false, useFastNativeRounding.get()))
        .build()
  }
  private val circularImageDecodeOptionsAntiAliased: ImageDecodeOptions by lazy {
    ImageDecodeOptions.newBuilder()
        .setBitmapTransformation(CircularBitmapTransformation(true, useFastNativeRounding.get()))
        .build()
  }

  override fun getDecodeOptions(antiAliased: Boolean): ImageDecodeOptions =
      if (antiAliased) circularImageDecodeOptionsAntiAliased else circularImageDecodeOptions
}

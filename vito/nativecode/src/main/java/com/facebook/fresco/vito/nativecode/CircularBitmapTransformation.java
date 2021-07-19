/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.nativecode;

import android.graphics.Bitmap;
import com.facebook.imagepipeline.nativecode.NativeRoundingFilter;
import com.facebook.imagepipeline.transformation.BitmapTransformation;
import com.facebook.imagepipeline.transformation.CircularTransformation;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class CircularBitmapTransformation implements BitmapTransformation, CircularTransformation {

  private final boolean mAntiAliased;

  private final boolean mUseFastNativeRounding;

  public CircularBitmapTransformation(boolean antiAliased) {
    this(antiAliased, false);
  }

  public CircularBitmapTransformation(boolean antiAliased, boolean useFastNativeRounding) {
    mAntiAliased = antiAliased;
    mUseFastNativeRounding = useFastNativeRounding;
  }

  @Override
  public void transform(Bitmap bitmap) {
    if (mUseFastNativeRounding) {
      NativeRoundingFilter.toCircleFast(bitmap, mAntiAliased);
    } else {
      NativeRoundingFilter.toCircle(bitmap, mAntiAliased);
    }
  }

  @Override
  public boolean modifiesTransparency() {
    return true;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) return false;
    CircularBitmapTransformation other = (CircularBitmapTransformation) obj;

    return mAntiAliased == other.mAntiAliased;
  }

  public boolean isAntiAliased() {
    return mAntiAliased;
  }

  @Override
  public int hashCode() {
    return mAntiAliased ? 1 : 0;
  }
}

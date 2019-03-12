/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.transformation;

import android.graphics.Bitmap;
import com.facebook.imagepipeline.nativecode.NativeRoundingFilter;
import com.facebook.imagepipeline.transformation.BitmapTransformation;

public class CircularBitmapTransformation implements BitmapTransformation {

  private final boolean mAntiAliased;

  public CircularBitmapTransformation(boolean antiAliased) {
    mAntiAliased = antiAliased;
  }

  @Override
  public void transform(Bitmap bitmap) {
    NativeRoundingFilter.toCircle(bitmap, mAntiAliased);
  }

  @Override
  public boolean modifiesTransparency() {
    return true;
  }

  @Override
  public boolean equals(Object obj) {
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

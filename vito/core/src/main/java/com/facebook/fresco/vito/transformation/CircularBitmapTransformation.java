/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.transformation;

import android.graphics.Bitmap;
import com.facebook.imagepipeline.transformation.BitmapTransformation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class CircularBitmapTransformation implements BitmapTransformation {

  private final boolean mAntiAliased;

  private final boolean mUseFastNativeRounding;

  private static Method sToCircle;

  private static Method sToCircleFast;

  static {
    try {
      Class<?> clazz = Class.forName("com.facebook.imagepipeline.nativecode.NativeRoundingFilter");
      sToCircle = clazz.getMethod("toCircle", Bitmap.class, boolean.class);
      sToCircleFast = clazz.getMethod("toCircleFast", Bitmap.class, boolean.class);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Native code for rounding unsupported", e);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException("Native code for rounding unsupported", e);
    }
  }

  public CircularBitmapTransformation(boolean antiAliased) {
    this(antiAliased, false);
  }

  public CircularBitmapTransformation(boolean antiAliased, boolean useFastNativeRounding) {
    mAntiAliased = antiAliased;
    mUseFastNativeRounding = useFastNativeRounding;
  }

  @Override
  public void transform(Bitmap bitmap) {
    try {
      if (mUseFastNativeRounding) {
        sToCircleFast.invoke(null, bitmap, mAntiAliased);
      } else {
        sToCircle.invoke(null, bitmap, mAntiAliased);
      }
    } catch (IllegalAccessException e) {
      throw new RuntimeException("Native code for rounding unsupported", e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException("Native code for rounding unsupported", e);
    }
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

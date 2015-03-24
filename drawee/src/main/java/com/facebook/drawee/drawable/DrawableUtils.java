/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawee.drawable;

import javax.annotation.Nullable;

import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

/**
 * Helper class containing functionality commonly used by drawables.
 */
public class DrawableUtils {

  /**
   * Copies various properties from one drawable to the other.
   * @param to drawable to copy properties to
   * @param from drawable to copy properties from
   */
  public static void copyProperties(Drawable to, Drawable from) {
    if (from == null || to == null || to == from) {
      return;
    }

    to.setBounds(from.getBounds());
    to.setChangingConfigurations(from.getChangingConfigurations());
    to.setLevel(from.getLevel());
    to.setVisible(from.isVisible(), /* restart */ false);
    to.setState(from.getState());
  }

  /**
   * Sets various paint properties on the drawable
   * @param drawable Drawable on which to set the properties
   * @param properties wrapper around property values to set on the drawable
   */
  public static void setDrawableProperties(Drawable drawable, DrawableProperties properties) {
    if (drawable == null || properties == null) {
      return;
    }

    drawable.setAlpha(properties.getAlpha());
    drawable.setColorFilter(properties.getColorFilter());
    drawable.setDither(properties.isDither());
    drawable.setFilterBitmap(properties.isFilterBitmap());
  }

  /**
   * Sets callback to the drawable.
   * @param drawable drawable to set callbacks to
   * @param callback standard Android Drawable.Callback
   * @param transformCallback TransformCallback used by TransformAwareDrawables
   */
  public static void setCallbacks(
      Drawable drawable,
      @Nullable Drawable.Callback callback,
      @Nullable TransformCallback transformCallback) {
    if (drawable != null) {
      drawable.setCallback(callback);
      if (drawable instanceof TransformAwareDrawable) {
        ((TransformAwareDrawable) drawable).setTransformCallback(transformCallback);
      }
    }
  }

  /**
   * Multiplies the color with the given alpha.
   * @param color color to be multiplied
   * @param alpha value between 0 and 255
   * @return multiplied color
   */
  public static int multiplyColorAlpha(int color, int alpha) {
    if (alpha == 255) {
      return color;
    }
    if (alpha == 0) {
      return color & 0x00FFFFFF;
    }
    alpha = alpha + (alpha >> 7); // make it 0..256
    int colorAlpha = color >>> 24;
    int multipliedAlpha = colorAlpha * alpha >> 8;
    return (multipliedAlpha << 24) | (color & 0x00FFFFFF);
  }

  /**
   * Gets the opacity from a color. Inspired by Android ColorDrawable.
   * @param color
   * @return opacity expressed by one of PixelFormat constants
   */
  public static int getOpacityFromColor(int color) {
    int colorAlpha = color >>> 24;
    if (colorAlpha == 255) {
      return PixelFormat.OPAQUE;
    } else if (colorAlpha == 0) {
      return PixelFormat.TRANSPARENT;
    } else {
      return PixelFormat.TRANSLUCENT;
    }
  }
}

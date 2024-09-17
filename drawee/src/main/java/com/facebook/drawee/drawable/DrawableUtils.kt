/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.drawable

import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

/** Helper class containing functionality commonly used by drawables. */
object DrawableUtils {

  /**
   * Clones the specified drawable.
   *
   * @param drawable the drawable to clone.
   * @return a clone of the drawable or null if the drawable cannot be cloned.
   */
  @JvmStatic
  fun cloneDrawable(drawable: Drawable?): Drawable? {
    drawable ?: return null

    return (drawable as? CloneableDrawable)?.cloneDrawable()
        ?: drawable.constantState?.newDrawable()
  }

  /**
   * Copies various properties from one drawable to the other.
   *
   * @param to drawable to copy properties to
   * @param from drawable to copy properties from
   */
  @JvmStatic
  fun copyProperties(to: Drawable?, from: Drawable?) {
    if (from == null || to == null || to === from) {
      return
    }
    to.bounds = from.bounds
    to.changingConfigurations = from.changingConfigurations
    to.setLevel(from.level)
    to.setVisible(from.isVisible, /* restart */ false)
    to.setState(from.state)
  }

  /**
   * Sets various paint properties on the drawable
   *
   * @param drawable Drawable on which to set the properties
   * @param properties wrapper around mValue values to set on the drawable
   */
  @JvmStatic
  fun setDrawableProperties(drawable: Drawable?, properties: DrawableProperties?) {
    drawable ?: return
    properties?.applyTo(drawable)
  }

  /**
   * Sets callback to the drawable.
   *
   * @param drawable drawable to set callbacks to
   * @param callback standard Android Drawable.Callback
   * @param transformCallback TransformCallback used by TransformAwareDrawables
   */
  @JvmStatic
  fun setCallbacks(
      drawable: Drawable?,
      callback: Drawable.Callback?,
      transformCallback: TransformCallback?
  ) {
    drawable ?: return
    drawable.callback = callback
    (drawable as? TransformAwareDrawable)?.setTransformCallback(transformCallback)
  }

  /**
   * Multiplies the color with the given alpha.
   *
   * @param color color to be multiplied
   * @param alpha value between 0 and 255
   * @return multiplied color
   */
  @JvmStatic
  fun multiplyColorAlpha(color: Int, alpha: Int): Int {
    if (alpha == 255) {
      return color
    }
    if (alpha == 0) {
      return color and 0x00FFFFFF
    }

    var clippedAlpha = alpha
    clippedAlpha += (clippedAlpha shr 7) // make it 0..256
    val colorAlpha = color ushr 24
    val multipliedAlpha = colorAlpha * clippedAlpha shr 8
    return multipliedAlpha shl 24 or (color and 0x00FFFFFF)
  }

  /**
   * Gets the opacity from a color. Inspired by Android ColorDrawable.
   *
   * @param color
   * @return opacity expressed by one of PixelFormat constants
   */
  @JvmStatic
  fun getOpacityFromColor(color: Int): Int =
      when (color ushr 24) {
        255 -> PixelFormat.OPAQUE
        0 -> PixelFormat.TRANSPARENT
        else -> PixelFormat.TRANSLUCENT
      }
}

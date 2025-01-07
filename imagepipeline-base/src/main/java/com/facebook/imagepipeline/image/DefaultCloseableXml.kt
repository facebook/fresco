/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.image

import android.graphics.drawable.Drawable

public class DefaultCloseableXml(private var drawable: Drawable?) :
    DefaultCloseableImage(), CloseableXml {
  private var closed = false

  override fun getSizeInBytes(): Int {
    return getWidth() * getHeight() * 4 // 4 bytes ARGB per pixel
  }

  override fun close() {
    drawable = null
    closed = true
  }

  override fun isClosed(): Boolean {
    return closed
  }

  override fun getWidth(): Int {
    return drawable?.intrinsicWidth?.takeIf { it >= 0 } ?: 0
  }

  override fun getHeight(): Int {
    return drawable?.intrinsicHeight?.takeIf { it >= 0 } ?: 0
  }

  override fun buildDrawable(): Drawable? {
    return drawable?.constantState?.newDrawable()
  }
}

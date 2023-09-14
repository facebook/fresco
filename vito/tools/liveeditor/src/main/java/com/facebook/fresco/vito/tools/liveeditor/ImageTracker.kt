/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.tools.liveeditor

import com.facebook.fresco.vito.core.FrescoDrawableInterface
import com.facebook.fresco.vito.core.impl.BaseVitoImagePerfListener
import java.lang.ref.WeakReference

open class ImageTracker : BaseVitoImagePerfListener() {
  private val drawables: MutableList<WeakReference<FrescoDrawableInterface>> = ArrayList()

  val drawableCount: Int
    get() = drawables.size

  fun getDrawableOrNull(index: Int): FrescoDrawableInterface? = drawables.getOrNull(index)?.get()

  fun reset() {
    drawables.clear()
  }

  private fun trackDrawable(drawable: FrescoDrawableInterface) {
    drawables.add(WeakReference(drawable))
  }

  private fun removeDrawable(drawable: FrescoDrawableInterface) {
    // Remove the Drawable and any null drawables that have been collected (weak reference)
    drawables.removeAll { it.get() == drawable || it.get() == null }
  }

  override fun onImageMount(drawable: FrescoDrawableInterface) {
    trackDrawable(drawable)
  }

  override fun onImageUnmount(drawable: FrescoDrawableInterface) {
    removeDrawable(drawable)
  }
}

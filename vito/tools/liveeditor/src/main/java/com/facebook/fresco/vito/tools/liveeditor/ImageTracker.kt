/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.tools.liveeditor

import com.facebook.fresco.vito.core.FrescoDrawable2
import com.facebook.fresco.vito.core.impl.FrescoController2Impl
import java.lang.ref.WeakReference

object ImageTracker : FrescoController2Impl.DrawableListener {
  override fun onNewDrawableCreated(drawable: FrescoDrawable2) {
    drawables.add(WeakReference(drawable))
  }

  private val drawables: MutableList<WeakReference<FrescoDrawable2>> = ArrayList()

  fun getDrawable(index: Int): FrescoDrawable2? {
    require(drawables.isNotEmpty()) { "No Drawables tracked!" }
    return if (index < drawables.size) drawables[index].get() else null
  }

  val drawableCount: Int
    get() = drawables.size

  fun reset() = drawables.clear()
}

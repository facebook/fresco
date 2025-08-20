/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.source

import android.content.res.Resources
import android.graphics.drawable.Drawable

@Suppress("KtDataClass")
data class DrawableResImageSource(private val resId: Int) : ImageSource {

  private var prefetchedDrawable: Drawable? = null

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (javaClass != other?.javaClass) {
      return false
    }

    other as DrawableResImageSource

    return resId == other.resId
  }

  override fun hashCode(): Int = resId

  override fun getClassNameString(): String = "DrawableResImageSource"

  fun prefetch(resources: Resources) {
    if (isPrefetchEnabled) {
      createPrefetchDrawable(resources)
    }
  }

  fun createDrawable(resources: Resources): Drawable {
    createPrefetchDrawable(resources)
    return prefetchedDrawable ?: throw IllegalStateException("Failed to create drawable")
  }

  private fun createPrefetchDrawable(resources: Resources) {
    if (prefetchedDrawable == null) {
      prefetchedDrawable = resources.getDrawable(resId)
    }
  }

  companion object {
    var isPrefetchEnabled: Boolean = false
  }
}

/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.litho.slideshow

import android.graphics.drawable.Drawable
import com.facebook.drawee.drawable.FadeDrawable
import com.facebook.fresco.vito.core.FrescoDrawableInterface
import java.util.TimerTask

class FrescoVitoSlideshowDrawable<T>(drawable1: T, drawable2: T, drawable3: T) :
    FadeDrawable(arrayOf<Drawable>(drawable1, drawable2, drawable3)) where
T : Drawable,
T : FrescoDrawableInterface {

  private var currentLayer = 0
  var timerTask: TimerTask? = null
  val currentImage: FrescoDrawableInterface
    get() = checkNotNull(getDrawable(currentLayer) as FrescoDrawableInterface?)

  val nextImage: FrescoDrawableInterface
    get() = checkNotNull(getDrawable(nextLayerIndex) as FrescoDrawableInterface?)

  val previousImage: FrescoDrawableInterface
    get() = checkNotNull(getDrawable(previousLayerIndex) as FrescoDrawableInterface?)

  private val nextLayerIndex: Int
    get() = (currentLayer + 1) % numberOfLayers

  private val previousLayerIndex: Int
    get() = (currentLayer - 1 + numberOfLayers) % numberOfLayers

  fun fadeToNext() {
    val prev = previousLayerIndex
    val next = nextLayerIndex
    beginBatchMode()
    fadeUpToLayer(next)
    hideLayerImmediately(prev)
    endBatchMode()
    currentLayer = next
  }

  override fun reset() {
    timerTask?.cancel()
    timerTask = null
    super.reset()
    currentLayer = 0
  }
}

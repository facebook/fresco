/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.animated.giflite.draw

import android.graphics.Bitmap
import com.facebook.imagepipeline.animated.base.AnimatedDrawableFrameInfo.DisposalMethod
import com.facebook.imagepipeline.animated.base.AnimatedImageFrame

/**
 * Simple wrapper for an animated image frame back by [MovieDrawer]. All [MovieFrame] for the same
 * [MovieAnimatedImage] will be backed by the same [MovieDrawer].
 */
class MovieFrame(
    private val movieDrawer: MovieDrawer,
    private val frameStart: Int,
    private val frameDuration: Int,
    private val frameWidth: Int,
    private val frameHeight: Int,
    val disposalMode: DisposalMethod
) : AnimatedImageFrame {

  override fun dispose() = Unit

  override fun renderFrame(w: Int, h: Int, bitmap: Bitmap) {
    movieDrawer.drawFrame(frameStart, w, h, bitmap)
  }

  override fun getDurationMs(): Int = frameDuration

  override fun getWidth(): Int = frameWidth

  override fun getHeight(): Int = frameHeight

  override fun getXOffset(): Int = 0

  override fun getYOffset(): Int = 0
}

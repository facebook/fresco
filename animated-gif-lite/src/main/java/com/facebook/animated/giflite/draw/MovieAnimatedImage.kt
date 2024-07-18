/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.animated.giflite.draw

import android.graphics.Bitmap
import android.graphics.Bitmap.Config
import android.graphics.Movie
import com.facebook.imagepipeline.animated.base.AnimatedDrawableFrameInfo
import com.facebook.imagepipeline.animated.base.AnimatedDrawableFrameInfo.BlendOperation
import com.facebook.imagepipeline.animated.base.AnimatedImage
import com.facebook.imagepipeline.animated.base.AnimatedImageFrame

/** Simple wrapper for an animated image backed by [Movie]. */
class MovieAnimatedImage
@JvmOverloads
constructor(
    private val frames: Array<MovieFrame>,
    private val _sizeInBytes: Int,
    private val _duration: Int,
    private val _loopCount: Int,
    animatedBitmapConfig: Bitmap.Config? = null
) : AnimatedImage {

  private val _frameDurations: IntArray = IntArray(frames.size)
  private val _animatedBitmapConfig: Bitmap.Config?

  init {

    var i = 0
    val N = frames.size
    while (i < N) {
      _frameDurations[i] = frames[i].durationMs
      i++
    }
    this._animatedBitmapConfig = animatedBitmapConfig
  }

  override fun dispose() = Unit

  override fun getWidth(): Int = frames[0].width

  override fun getHeight(): Int = frames[0].height

  override fun getFrameCount(): Int = frames.size

  override fun getDuration(): Int = _duration

  override fun getFrameDurations(): IntArray = _frameDurations

  override fun getLoopCount(): Int = _loopCount

  override fun getFrame(frameNumber: Int): AnimatedImageFrame = frames[frameNumber]

  override fun doesRenderSupportScaling(): Boolean = true

  override fun getSizeInBytes(): Int = _sizeInBytes

  override fun getFrameInfo(frameNumber: Int): AnimatedDrawableFrameInfo {
    val frame = frames[frameNumber]
    return AnimatedDrawableFrameInfo(
        frameNumber,
        frame.xOffset,
        frame.yOffset,
        frame.width,
        frame.height,
        AnimatedDrawableFrameInfo.BlendOperation.BLEND_WITH_PREVIOUS,
        frames[frameNumber].disposalMode)
  }

  override fun getAnimatedBitmapConfig(): Bitmap.Config? = _animatedBitmapConfig
}

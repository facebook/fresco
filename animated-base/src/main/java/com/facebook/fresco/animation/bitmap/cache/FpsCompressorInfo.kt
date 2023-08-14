/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.bitmap.cache

import android.graphics.Bitmap
import com.facebook.common.references.CloseableReference
import com.facebook.imagepipeline.animated.base.AnimatedImage
import java.util.concurrent.TimeUnit

class FpsCompressorInfo(private val maxFpsLimit: Int) {

  /**
   * @param frameBitmaps has the bitmaps of the animation. FrameNumber -> Bitmap
   * @param targetFps compressed animation fps
   * @return AnimationFrame reference if the animation was saved in memory. Returns null if
   *   animation couldn't be allocated
   */
  fun compress(
      animatedImage: AnimatedImage,
      frameBitmaps: Map<Int, CloseableReference<Bitmap>>,
      targetFps: Int
  ): CompressionResult {
    val sanitiseFps = targetFps.coerceAtLeast(1).coerceAtMost(maxFpsLimit)

    val realToCompressIndex =
        calculateReducedIndexes(animatedImage.duration, frameBitmaps.size, sanitiseFps)
    return compressAnimation(frameBitmaps, realToCompressIndex)
  }

  /**
   * Create a Map<AssetBitmapIndex, CompressBitmapIndex> calculated based on the maximum FPS allowed
   *
   * @param duration duration of the animation in ms
   * @param frameCount Number of frames extracted from the animation asset
   * @param targetFps
   * @return Map of equivalences between the original frame number and compress frame number
   */
  private fun calculateReducedIndexes(
      duration: Int,
      frameCount: Int,
      targetFps: Int
  ): Map<Int, Int> {
    val maxAllowedFrames =
        targetFps.times(TimeUnit.MILLISECONDS.toSeconds(duration.toLong())).coerceAtLeast(0)

    return if (maxAllowedFrames >= frameCount) {
      (0 until frameCount).associateWith { it }
    } else {
      val offset = maxAllowedFrames.div(frameCount.toFloat())
      (0 until frameCount).associateWith { it.times(offset).toInt() }
    }
  }

  /**
   * Compress animation releasing those bitmaps which wont be in the final animation
   *
   * @param frameBitmaps relation frameNumber->bitmap of the original animation
   * @param realToReducedIndex map of equivalences between originalFrameNumber->compressFrameNumber
   * @return map associating compressFrameNumber->bitmap
   */
  private fun compressAnimation(
      frameBitmaps: Map<Int, CloseableReference<Bitmap>>,
      realToReducedIndex: Map<Int, Int>
  ): CompressionResult {
    val compressedAnim = mutableMapOf<Int, CloseableReference<Bitmap>>()
    val removedFrames = mutableListOf<CloseableReference<Bitmap>>()

    frameBitmaps.forEach { (i, bitmapRef) ->
      val reducedIndex = realToReducedIndex[i] ?: return@forEach

      if (compressedAnim.contains(reducedIndex)) {
        removedFrames.add(bitmapRef)
      } else {
        compressedAnim[reducedIndex] = bitmapRef
      }
    }

    return CompressionResult(compressedAnim, realToReducedIndex, removedFrames)
  }

  /**
   * Contain the result of the compression
   *
   * @param compressedAnim Contains the bitmaps associated with frame number
   *
   * ```
   *  ________________________
   * | Frame 1 ----> Bitmap1 |             ________________________
   * | Frame 2 ----> Bitmap2 |   ====>    | Compress 1 ----> Bitmap1 |
   * -------------------------            -------------------------
   * ```
   *
   * @param realToReducedIndex contains the association between old frame numbers to new frame
   *   numbers
   *
   * ```
   *  ________________________________
   * | Frame 1 ----> Compress Frame 1 |
   * | Frame 2 ----> Compress Frame 1 |
   * ---------------------------------
   *
   * ```
   *
   * @param removedFrames contains the bitmap that are not useful. "Bitmap2"
   */
  class CompressionResult(
      val compressedAnim: Map<Int, CloseableReference<Bitmap>>,
      val realToReducedIndex: Map<Int, Int>,
      val removedFrames: List<CloseableReference<Bitmap>>
  )
}

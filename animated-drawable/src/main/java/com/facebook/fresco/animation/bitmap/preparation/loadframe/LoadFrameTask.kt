/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.bitmap.preparation.loadframe

import android.graphics.Bitmap
import com.facebook.common.references.CloseableReference
import com.facebook.fresco.animation.bitmap.BitmapFrameRenderer
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory

/**
 * This task render and save in a Map all the bitmap frames from Frame 0 until [untilFrame]. Once
 * the render is finished, the bitmaps are provided via [output]
 */
class LoadFrameTask(
    private val width: Int,
    private val height: Int,
    private val untilFrame: Int,
    override val priority: Int,
    private val output: LoadFrameOutput,
    private val platformBitmapFactory: PlatformBitmapFactory,
    private val bitmapFrameRenderer: BitmapFrameRenderer,
) : LoadFramePriorityTask {

  private val bitmapConfig = Bitmap.Config.ARGB_8888

  override fun run() {
    val frameCollection = mutableMapOf<Int, CloseableReference<Bitmap>>()
    val canvasBitmapFrame = platformBitmapFactory.createBitmap(width, height, bitmapConfig)

    // Animation frames have to be render incrementally
    (0 until untilFrame).forEach { frameNumber ->
      // Render frame
      val renderSucceed = bitmapFrameRenderer.renderFrame(frameNumber, canvasBitmapFrame.get())
      if (!renderSucceed) {
        return@forEach
      }

      // Save frame
      val copyFrame = platformBitmapFactory.createBitmap(canvasBitmapFrame.get())
      frameCollection[frameNumber] = copyFrame
    }

    canvasBitmapFrame.close()
    output(frameCollection)
  }
}

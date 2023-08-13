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
import com.facebook.fresco.animation.bitmap.preparation.loadframe.LoadFramePriorityTask.Priority
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory

/**
 * Render the frame given [frameNumber]. We have to find the nearest cached bitmap using
 * [getCachedBitmap] to start with, and the apply deltas until to render the [frameNumber] desired.
 * The final bitmap will be sent via [output]
 */
class LoadOnDemandFrameTask(
    private val frameNumber: Int,
    private val getCachedBitmap: (Int) -> CloseableReference<Bitmap>?,
    override val priority: Priority,
    private val output: (CloseableReference<Bitmap>?) -> Unit,
    private val platformBitmapFactory: PlatformBitmapFactory,
    private val bitmapFrameRenderer: BitmapFrameRenderer,
) : LoadFramePriorityTask {

  override fun run() {
    val nearestFrame =
        (frameNumber downTo 0)
            .asSequence()
            .mapNotNull {
              val bitmap = getCachedBitmap(it) ?: return@mapNotNull null
              Pair(it, bitmap)
            }
            .firstOrNull()
            ?: return exit(null)

    val canvasBitmap = platformBitmapFactory.createBitmap(nearestFrame.second.get())
    (nearestFrame.first + 1..frameNumber).forEach {
      bitmapFrameRenderer.renderFrame(it, canvasBitmap.get())
    }
    exit(canvasBitmap)
  }

  private fun exit(bitmap: CloseableReference<Bitmap>?) {
    output.invoke(bitmap)
  }
}

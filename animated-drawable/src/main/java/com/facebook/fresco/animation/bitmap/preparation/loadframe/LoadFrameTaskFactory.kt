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

/** This factory provides task for the animation and set the priority on each task */
class LoadFrameTaskFactory(
    private val platformBitmapFactory: PlatformBitmapFactory,
    private val bitmapFrameRenderer: BitmapFrameRenderer,
) {

  fun createFirstFrameTask(
      width: Int,
      height: Int,
      output: LoadFrameOutput,
  ): LoadFrameTask {
    return LoadFrameTask(
        width = width,
        height = height,
        untilFrame = 1,
        priority = LOAD_FIRST_FRAME_PRIORITY,
        output = output,
        platformBitmapFactory = platformBitmapFactory,
        bitmapFrameRenderer = bitmapFrameRenderer)
  }

  fun createLoadFullAnimationTask(
      width: Int,
      height: Int,
      frameCount: Int,
      output: LoadFrameOutput,
  ): LoadFrameTask {
    return LoadFrameTask(
        width = width,
        height = height,
        untilFrame = frameCount,
        priority = LOAD_FULL_FRAMES_PRIORITY,
        output = output,
        platformBitmapFactory = platformBitmapFactory,
        bitmapFrameRenderer = bitmapFrameRenderer)
  }

  fun createOnDemandTask(
      frameNumber: Int,
      getCachedBitmap: (Int) -> CloseableReference<Bitmap>?,
      output: (CloseableReference<Bitmap>?) -> Unit,
  ): LoadOnDemandFrameTask {
    return LoadOnDemandFrameTask(
        frameNumber = frameNumber,
        getCachedBitmap = getCachedBitmap,
        priority = LOAD_ON_DEMAND_PRIORITY,
        output = output,
        platformBitmapFactory = platformBitmapFactory,
        bitmapFrameRenderer = bitmapFrameRenderer,
    )
  }

  companion object {
    private const val LOAD_FIRST_FRAME_PRIORITY = 10
    private const val LOAD_ON_DEMAND_PRIORITY = 5
    private const val LOAD_FULL_FRAMES_PRIORITY = 1
  }
}

interface LoadFrameOutput {
  fun onSuccess(frames: Map<Int, CloseableReference<Bitmap>>)

  fun onFail()
}

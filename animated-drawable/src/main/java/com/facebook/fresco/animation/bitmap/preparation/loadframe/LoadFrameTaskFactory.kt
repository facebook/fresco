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
        priority = Priority.HIGH,
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
        priority = Priority.LOW,
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
        priority = Priority.MEDIUM,
        output = output,
        platformBitmapFactory = platformBitmapFactory,
        bitmapFrameRenderer = bitmapFrameRenderer,
    )
  }
}

interface LoadFrameOutput {
  fun onSuccess(frames: Map<Int, CloseableReference<Bitmap>>)

  fun onFail()
}

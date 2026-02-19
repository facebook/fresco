/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.bitmap.preparation.ondemandanimation

import android.graphics.Bitmap
import androidx.annotation.UiThread
import com.facebook.common.references.CloseableReference
import com.facebook.fresco.animation.backend.AnimationInformation

/**
 * Callback interface for logging when frame loading encounters zero dimensions. This allows
 * app-specific error reporting implementations to be injected.
 */
fun interface ZeroFrameDimensionsListener {
  /**
   * Called when frame loading is skipped due to zero frame dimensions.
   *
   * @param origin the origin of the call (e.g., "BufferFrameLoader.getFrame")
   * @param frameNumber the frame number being loaded (-1 if not applicable)
   * @param width the frame width
   * @param height the frame height
   */
  fun onZeroFrameDimensions(origin: String, frameNumber: Int, width: Int, height: Int)
}

/** This interface provides the basic O(1) methods to extract and prepare bitmap animations */
interface FrameLoader {

  /** Animation info */
  val animationInformation: AnimationInformation

  /**
   * Return the [frameNumber] bitmap to render, given the [width] and [height] of the view.
   *
   * This method is always executed by the main thread, so its performance needs to be O(1)
   */
  @UiThread fun getFrame(frameNumber: Int, width: Int, height: Int): FrameResult

  /**
   * Prepare the frames to be rendered. [onAnimationLoaded] is executed once the preparation is done
   *
   * This method is always executed by the main thread, so its performance needs to be O(1).
   */
  @UiThread fun prepareFrames(width: Int, height: Int, onAnimationLoaded: () -> Unit)

  /**
   * Force the animation to run to indicated fps
   *
   * @param fps fps that animation should run
   */
  fun compressToFps(fps: Int): Unit = Unit

  fun onStop() = Unit

  /** Release resources */
  fun clear()
}

class FrameResult(val bitmapRef: CloseableReference<Bitmap>?, val type: FrameType) {
  enum class FrameType {
    SUCCESS,
    NEAREST,
    MISSING,
  }
}

/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.animated.webp

import android.graphics.Bitmap
import com.facebook.common.internal.DoNotStrip
import com.facebook.imagepipeline.animated.base.AnimatedImageFrame
import javax.annotation.concurrent.ThreadSafe

/** A single frame of a [WebPImage]. */
@ThreadSafe
open class WebPFrame
/**
 * Constructs the frame with the native pointer. This is called by native code.
 *
 * @param nativeContext the native pointer
 */
@DoNotStrip
internal constructor( // Accessed by native methods
    @field:DoNotStrip @field:Suppress("unused") private var mNativeContext: Long,
) : AnimatedImageFrame {

  // This is a valid use of finalize. No other mechanism is appropriate.
  protected fun finalize() {
    nativeFinalize()
  }

  override fun dispose() {
    nativeDispose()
  }

  override fun renderFrame(width: Int, height: Int, bitmap: Bitmap) {
    nativeRenderFrame(width, height, bitmap)
  }

  override fun getDurationMs(): Int = nativeGetDurationMs()

  override fun getWidth(): Int = nativeGetWidth()

  override fun getHeight(): Int = nativeGetHeight()

  override fun getXOffset(): Int = nativeGetXOffset()

  override fun getYOffset(): Int = nativeGetYOffset()

  fun shouldDisposeToBackgroundColor(): Boolean = nativeShouldDisposeToBackgroundColor()

  val isBlendWithPreviousFrame: Boolean
    get() = nativeIsBlendWithPreviousFrame()

  private external fun nativeRenderFrame(width: Int, height: Int, bitmap: Bitmap)

  private external fun nativeGetDurationMs(): Int

  private external fun nativeGetWidth(): Int

  private external fun nativeGetHeight(): Int

  private external fun nativeGetXOffset(): Int

  private external fun nativeGetYOffset(): Int

  private external fun nativeShouldDisposeToBackgroundColor(): Boolean

  private external fun nativeIsBlendWithPreviousFrame(): Boolean

  private external fun nativeDispose()

  private external fun nativeFinalize()
}

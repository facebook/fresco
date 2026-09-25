/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core

import android.graphics.Rect
import android.graphics.drawable.Drawable
import com.facebook.common.callercontext.ContextChain
import com.facebook.fresco.ui.common.ImagePerfDataListener
import com.facebook.fresco.ui.common.OnFadeListener
import com.facebook.fresco.vito.listener.ImageListener

interface FrescoController2 {

  fun <T> createDrawable(): T where T : Drawable, T : FrescoDrawableInterface = createDrawable(null)

  fun <T> createDrawable(uiFramework: String?): T where T : Drawable, T : FrescoDrawableInterface

  /**
   * Fetches [imageRequest] into [drawable].
   *
   * @param contextChain consumed by wrapping controllers; the bundled implementations use
   *   [callerContext] only.
   * @param onFadeListener notified when the actual image fades in or is shown immediately.
   *   Implementations must forward it to the drawable and clear it on reset/close.
   * @return true if the image was set immediately, false if it was queued for later. Returning
   *   false for an unsupported drawable type is allowed; callers must branch on it. Failure to
   *   decode into a drawable (unmapped image type) is reported through the image listeners, never
   *   by throwing.
   */
  fun fetch(
      drawable: FrescoDrawableInterface,
      imageRequest: VitoImageRequest,
      callerContext: Any?,
      contextChain: ContextChain?,
      listener: ImageListener?,
      perfDataListener: ImagePerfDataListener? = null,
      onFadeListener: OnFadeListener?,
      viewportDimensions: Rect?,
      vitoImageRequestListener: VitoImageRequestListener? = null,
  ): Boolean

  fun releaseDelayed(drawable: FrescoDrawableInterface)

  fun releaseNextFrame(drawable: FrescoDrawableInterface)

  fun releaseImmediately(drawable: FrescoDrawableInterface)
}

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

  fun release(drawable: FrescoDrawableInterface)

  fun releaseImmediately(drawable: FrescoDrawableInterface)
}

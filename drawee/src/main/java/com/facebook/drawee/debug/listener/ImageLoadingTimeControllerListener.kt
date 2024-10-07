/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.debug.listener

import android.graphics.drawable.Animatable
import com.facebook.drawee.controller.BaseControllerListener

/**
 * Currently we are measuring this from Submit to Final Image.But can be extended to include
 * intermediate time and failure cases also
 */
class ImageLoadingTimeControllerListener(
    private val imageLoadingTimeListener: ImageLoadingTimeListener?
) : BaseControllerListener<Any?>() {

  private var requestSubmitTimeMs = -1L
  private var finalImageSetTimeMs = -1L

  override fun onSubmit(id: String, callerContext: Any?) {
    requestSubmitTimeMs = System.currentTimeMillis()
  }

  override fun onFinalImageSet(id: String, imageInfo: Any?, animatable: Animatable?) {
    finalImageSetTimeMs = System.currentTimeMillis()
    imageLoadingTimeListener?.onFinalImageSet(finalImageSetTimeMs - requestSubmitTimeMs)
  }
}

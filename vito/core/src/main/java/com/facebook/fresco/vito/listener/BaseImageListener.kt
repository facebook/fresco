/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.fresco.vito.listener

import android.graphics.drawable.Drawable
import com.facebook.drawee.backends.pipeline.info.ImageOrigin
import com.facebook.fresco.ui.common.DimensionsInfo
import com.facebook.imagepipeline.image.ImageInfo
import com.facebook.infer.annotation.OkToExtend

@OkToExtend
open class BaseImageListener : ImageListener {

  override fun onSubmit(id: Long, callerContext: Any?) {}

  override fun onPlaceholderSet(id: Long, placeholder: Drawable?) {}

  override fun onFinalImageSet(
      id: Long,
      @ImageOrigin imageOrigin: Int,
      imageInfo: ImageInfo?,
      drawable: Drawable?
  ) {}

  override fun onIntermediateImageSet(id: Long, imageInfo: ImageInfo?) {}

  override fun onIntermediateImageFailed(id: Long, throwable: Throwable?) {}

  override fun onFailure(id: Long, error: Drawable?, throwable: Throwable?) {}

  override fun onRelease(id: Long) {}

  override fun onImageDrawn(id: String, imageInfo: ImageInfo, dimensionsInfo: DimensionsInfo) {}
}

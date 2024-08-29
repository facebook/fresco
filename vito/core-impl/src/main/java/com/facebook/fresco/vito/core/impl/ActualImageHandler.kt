/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import android.content.res.Resources
import android.graphics.drawable.Drawable
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.fresco.vito.renderer.DrawableImageDataModel
import com.facebook.fresco.vito.renderer.ImageDataModel
import com.facebook.imagepipeline.image.CloseableImage

fun ImageLayerDataModel.setActualImage(
    resources: Resources,
    imageOptions: ImageOptions,
    closeableImage: CloseableImage,
    imageToDataModelMapper: (Resources, CloseableImage, ImageOptions) -> ImageDataModel?
) {
  configure(
      dataModel = imageToDataModelMapper(resources, closeableImage, imageOptions),
      canvasTransformation = imageOptions.createActualImageCanvasTransformation(),
      roundingOptions = imageOptions.roundingOptions,
      borderOptions = imageOptions.borderOptions,
      colorFilter = imageOptions.actualImageColorFilter)
}

fun ImageLayerDataModel.setActualImageDrawable(
    imageOptions: ImageOptions,
    actualImageDrawable: Drawable
) {
  configure(
      dataModel = DrawableImageDataModel(actualImageDrawable),
      canvasTransformation = imageOptions.createActualImageCanvasTransformation(),
      roundingOptions = imageOptions.roundingOptions,
      borderOptions = imageOptions.borderOptions,
      colorFilter = imageOptions.actualImageColorFilter)
}

fun ImageLayerDataModel.setPlaceholder(resources: Resources, imageOptions: ImageOptions) {
  val model = imageOptions.createPlaceholderModel(resources)
  if (model == null) {
    reset()
    return
  }
  configure(
      dataModel = model,
      canvasTransformation = imageOptions.createPlaceholderCanvasTransformation(),
      roundingOptions =
          if (imageOptions.placeholderApplyRoundingOptions) imageOptions.roundingOptions else null,
      borderOptions =
          if (imageOptions.placeholderApplyRoundingOptions) imageOptions.borderOptions else null)
}

fun ImageLayerDataModel.setOverlay(resources: Resources, imageOptions: ImageOptions) {
  configure(dataModel = imageOptions.createOverlayModel(resources))
}

fun ImageLayerDataModel.setError(resources: Resources, imageOptions: ImageOptions) {
  val model = imageOptions.createErrorModel(resources)
  if (model == null) {
    reset()
    return
  }
  configure(
      dataModel = model, canvasTransformation = imageOptions.createErrorCanvasTransformation())
}

/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import android.content.res.Resources
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.fresco.vito.renderer.ImageDataModel
import com.facebook.imagepipeline.image.CloseableImage

fun ImageLayerDataModel.setActualImage(
    imageOptions: ImageOptions,
    closeableImage: CloseableImage,
    imageToDataModelMapper: (CloseableImage, ImageOptions) -> ImageDataModel?
) {
  configure(
      dataModel = imageToDataModelMapper(closeableImage, imageOptions),
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

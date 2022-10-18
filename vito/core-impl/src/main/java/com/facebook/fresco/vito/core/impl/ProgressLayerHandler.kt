/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import android.content.res.Resources
import android.graphics.drawable.Drawable
import com.facebook.common.references.CloseableReference
import com.facebook.datasource.DataSource
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.fresco.vito.renderer.DrawableImageDataModel
import com.facebook.imagepipeline.image.CloseableImage
import kotlin.math.roundToInt

fun KFrescoVitoDrawable.setupProgressLayer(resources: Resources, imageOptions: ImageOptions) {
  val drawable = imageOptions.createProgressDrawable(resources)
  if (drawable == null) {
    progressLayer?.reset()
    progressLayer = null
    return
  }
  var layer = progressLayer
  if (layer == null) {
    layer = createLayer()
    progressLayer = layer
  }
  drawable.setProgress(0f)
  layer.configure(
      dataModel = DrawableImageDataModel(drawable),
      canvasTransformation = imageOptions.createProgressCanvasTransformation())
}

fun KFrescoVitoDrawable.hideProgressLayer() {
  progressLayer?.reset()
}

fun KFrescoVitoDrawable.updateProgress(dataSource: DataSource<CloseableReference<CloseableImage>>) {
  progressLayer?.getDataModel().maybeGetDrawable()?.apply {
    if (dataSource.isFinished) {
      return@apply
    }
    setProgress(dataSource.progress)
  }
}

/** Set the progress for a given progress Drawable. The value must be between 0 and 1 */
fun Drawable.setProgress(progress: Float) {
  level = (progress * 10000).roundToInt()
  toggleAnimation(level <= 9990)
}

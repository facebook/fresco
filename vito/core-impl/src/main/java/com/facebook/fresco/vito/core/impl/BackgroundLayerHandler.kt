/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import android.content.res.Resources
import com.facebook.fresco.vito.options.ImageOptions

fun KFrescoVitoDrawable.setupBackgroundLayer(resources: Resources, imageOptions: ImageOptions) {
  val backgroundModel = imageOptions.createBackgroundModel(resources)
  if (backgroundModel == null) {
    backgroundLayer?.reset()
    backgroundLayer = null
    return
  }
  var layer = backgroundLayer
  if (layer == null) {
    layer = createLayer()
    backgroundLayer = layer
  }
  layer.configure(
      dataModel = backgroundModel,
      canvasTransformation = imageOptions.createActualImageCanvasTransformation(),
      roundingOptions = imageOptions.roundingOptions,
  )
}

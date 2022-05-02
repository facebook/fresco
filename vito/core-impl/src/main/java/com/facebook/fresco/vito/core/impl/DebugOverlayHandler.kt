/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import com.facebook.common.internal.Supplier
import com.facebook.fresco.vito.core.impl.debug.DebugOverlayDrawable
import com.facebook.fresco.vito.renderer.DrawableImageDataModel

class DebugOverlayHandler(
    private val isEnabled: Supplier<Boolean>,
    private val debugDataProviders: List<DebugDataProvider> =
        listOf(imageIDProvider, drawableDimensionsProvider, imageDimensionsProvider)
) {

  fun update(drawable: KFrescoVitoDrawable) {
    if (!isEnabled.get()) {
      return
    }

    extractDebugOverlayDrawable(drawable).apply {
      debugDataProviders.forEach { addDebugData(it.shortName, it.extractData(drawable)) }
    }
  }

  private fun extractDebugOverlayDrawable(drawable: KFrescoVitoDrawable): DebugOverlayDrawable {
    val model = drawable.debugOverlayImageLayer?.getDataModel()
    if (model == null ||
        model !is DrawableImageDataModel ||
        model.drawable !is DebugOverlayDrawable) {
      val debugOverlayDrawable = DebugOverlayDrawable("K")
      drawable.debugOverlayImageLayer =
          ImageLayerDataModel(drawable.callbackProvider).apply {
            configure(dataModel = DrawableImageDataModel(debugOverlayDrawable))
          }
      return debugOverlayDrawable
    }
    return model.drawable as DebugOverlayDrawable
  }
}

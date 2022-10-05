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
        listOf(
            imageIDProvider,
            drawableDimensionsProvider,
            imageDimensionsProvider,
            imageOriginProvider,
            imageOriginSubcategoryProvider)
) {

  fun update(drawable: KFrescoVitoDrawable) {
    if (!isEnabled.get()) {
      return
    }

    val debugOverlayDrawable = extractDebugOverlayDrawable(drawable)
    debugDataProviders.forEach {
      when (it) {
        is StringDebugDataProvider ->
            debugOverlayDrawable.addDebugData(it.shortName, it.extractData(drawable))
        is StringAndColorDebugDataProvider -> {
          val (data, color) = it.extractDataAndColor(drawable)
          debugOverlayDrawable.addDebugData(it.shortName, data, color)
        }
      }
    }
  }

  private fun extractDebugOverlayDrawable(drawable: KFrescoVitoDrawable): DebugOverlayDrawable {
    val model = drawable.debugOverlayImageLayer?.getDataModel()
    if (model == null ||
        model !is DrawableImageDataModel ||
        model.drawable !is DebugOverlayDrawable) {
      val debugOverlayDrawable = DebugOverlayDrawable("K")
      drawable.debugOverlayImageLayer =
          drawable.createLayer().apply {
            configure(dataModel = DrawableImageDataModel(debugOverlayDrawable))
          }
      return debugOverlayDrawable
    }
    return model.drawable as DebugOverlayDrawable
  }
}

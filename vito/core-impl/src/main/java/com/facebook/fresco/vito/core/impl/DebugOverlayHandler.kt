/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

@file:SuppressLint("HexColorValueUsage")

package com.facebook.fresco.vito.core.impl

import android.annotation.SuppressLint
import com.facebook.common.internal.Supplier
import com.facebook.fresco.vito.core.impl.debug.DebugDataProvider
import com.facebook.fresco.vito.core.impl.debug.DebugLogcatReporter
import com.facebook.fresco.vito.core.impl.debug.DebugOverlayDrawable
import com.facebook.fresco.vito.core.impl.debug.ImageFitRatioConfig
import com.facebook.fresco.vito.core.impl.debug.StringAndColorDebugDataProvider
import com.facebook.fresco.vito.core.impl.debug.StringDebugDataProvider
import com.facebook.fresco.vito.core.impl.debug.drawableDimensionsProvider
import com.facebook.fresco.vito.core.impl.debug.hdrGainmapProvider
import com.facebook.fresco.vito.core.impl.debug.imageIDProvider
import com.facebook.fresco.vito.renderer.DrawableImageDataModel

class DebugOverlayHandler(
    private val isEnabled: Supplier<Boolean>,
    private val debugDataProviders: List<DebugDataProvider> = buildList {
      add(imageIDProvider)
      add(drawableDimensionsProvider)
      add(imageDimensionsProvider)
      add(createImageFitRatioProvider(ImageFitRatioConfig()))
      add(createWastedMemoryProvider(ImageFitRatioConfig()))
      add(imageOriginProvider)
      add(imageOriginSubcategoryProvider)
      add(hdrGainmapProvider)
    },
) {
  constructor(isEnabled: Boolean) : this(Supplier { isEnabled }) {}

  fun update(drawable: KFrescoVitoDrawable) {
    if (!isEnabled.get()) {
      return
    }

    val debugOverlayDrawable = extractDebugOverlayDrawable(drawable)
    drawable.onBoundsChangedCallback = { update(drawable) }
    debugOverlayDrawable.reset()
    debugDataProviders.forEach {
      when (it) {
        is StringDebugDataProvider ->
            debugOverlayDrawable.addDebugData(it.shortName, it.extractData(drawable))
        is StringAndColorDebugDataProvider -> {
          val (data, color) = it.extractDataAndColor(drawable)
          debugOverlayDrawable.addDebugData(it.shortName, data, color)
          if (it.shortName == "I:D" && data.isNotEmpty()) {
            debugOverlayDrawable.backgroundColor = (0x50 shl 24) or (color and 0x00FFFFFF)
          }
        }
      }
    }

    // Log image sizing data to logcat for programmatic capture
    val model = drawable.actualImageLayer.getDataModel()
    if (model != null) {
      val viewport = drawable.viewportDimensions
      val targetWidth =
          if (viewport != null && viewport.width() > 0) viewport.width()
          else drawable.bounds.width()
      val targetHeight =
          if (viewport != null && viewport.height() > 0) viewport.height()
          else drawable.bounds.height()
      val origin = drawable.extractOriginExtras()?.get("origin")?.toString()
      DebugLogcatReporter.maybeReport(
          imageId = drawable.imageId,
          uri = drawable.imageRequest?.finalImageRequest?.sourceUri?.toString(),
          imgW = model.width,
          imgH = model.height,
          drawW = targetWidth,
          drawH = targetHeight,
          origin = origin,
      )
    }
  }

  private fun extractDebugOverlayDrawable(drawable: KFrescoVitoDrawable): DebugOverlayDrawable {
    val model = drawable.debugOverlayImageLayer?.getDataModel()
    if (
        model == null || model !is DrawableImageDataModel || model.drawable !is DebugOverlayDrawable
    ) {
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

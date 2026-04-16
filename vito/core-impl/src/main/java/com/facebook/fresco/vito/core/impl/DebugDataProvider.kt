/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

@file:SuppressLint("ColorConstantUsageIssue")

package com.facebook.fresco.vito.core.impl

import android.annotation.SuppressLint
import android.graphics.Color
import com.facebook.fresco.middleware.HasExtraData
import com.facebook.fresco.ui.common.ControllerListener2.Extras
import com.facebook.fresco.vito.core.FrescoDrawableInterface
import com.facebook.fresco.vito.core.impl.debug.DebugOverlayImageOriginColor
import com.facebook.fresco.vito.core.impl.debug.ImageFitRatioConfig
import com.facebook.fresco.vito.core.impl.debug.StringAndColorDebugDataProvider
import com.facebook.fresco.vito.core.impl.debug.StringDebugDataProvider
import com.facebook.fresco.vito.core.impl.debug.computeImageFitRatioAndColor
import com.facebook.fresco.vito.core.impl.debug.computeWastedMemoryAndColor
import com.facebook.fresco.vito.core.impl.debug.formatDimensions
import com.facebook.fresco.vito.core.impl.debug.getOriginExtras
import com.facebook.fresco.vito.renderer.BitmapImageDataModel
import com.facebook.imageutils.BitmapUtil

// ============================================================================
// KFrescoVitoDrawable-specific providers
// ============================================================================

/** Provides the current progressive JPEG scan number from CloseableImage extras. */
val scanNumberProvider =
    StringDebugDataProvider(
        "S",
        "Scan Number",
        "The current progressive JPEG scan number",
    ) { drawable, _ ->
      if (drawable is KFrescoVitoDrawable) {
        val scanNumber =
            drawable.dataSource?.extras?.get("last_scan_num") as? Int
                ?: drawable.obtainExtras().imageExtras?.get("last_scan_num") as? Int
        if (scanNumber != null && scanNumber > 0) "S:$scanNumber" else ""
      } else {
        ""
      }
    }

/** Provides image dimensions for KFrescoVitoDrawable. */
val imageDimensionsProvider =
    StringDebugDataProvider("I", "Image dimensions", "The dimensions of the decoded image") {
        drawable,
        _ ->
      if (drawable is KFrescoVitoDrawable) {
        when (val model = drawable.actualImageLayer.getDataModel()) {
          null -> "unset"
          else -> formatDimensions(model.width, model.height)
        }
      } else {
        ""
      }
    }

internal fun KFrescoVitoDrawable.extractOriginExtras(): Map<String, Any>? =
    dataSource?.extras
        ?:
        // We did not receive data source extras, so the image did not come from the image
        // pipeline but from the bitmap memory cache shortcut
        obtainExtras().shortcutExtras

/** Gets the origin extras from the drawable or the provided extras. */
private fun getOriginExtrasFromDrawable(
    drawable: FrescoDrawableInterface,
    extras: Extras?,
): Map<String, Any>? {
  // First try to get from provided extras
  getOriginExtras(extras)?.let {
    return it
  }

  // Fall back to extracting from KFrescoVitoDrawable
  if (drawable is KFrescoVitoDrawable) {
    return drawable.extractOriginExtras()
  }

  return null
}

/** Provides image origin with color for KFrescoVitoDrawable. */
val imageOriginProvider =
    StringAndColorDebugDataProvider("o", "Origin", "The source of the image") { drawable, _ ->
      if (drawable is KFrescoVitoDrawable) {
        val origin = drawable.extractOriginExtras()?.get("origin")?.toString() ?: "unknown"
        val color = DebugOverlayImageOriginColor.getImageOriginColor(origin)

        origin to color
      } else {
        "" to Color.WHITE
      }
    }

/** Provides image origin subcategory with color for KFrescoVitoDrawable. */
val imageOriginSubcategoryProvider =
    StringAndColorDebugDataProvider(
        "o_s",
        "Origin Subcategory",
        "The subcategory of source of the image",
    ) { drawable, _ ->
      if (drawable is KFrescoVitoDrawable) {
        val originSubcategory =
            drawable.extractOriginExtras()?.get(HasExtraData.KEY_ORIGIN_SUBCATEGORY)?.toString()
                ?: "unknown"

        originSubcategory to Color.GRAY
      } else {
        "" to Color.WHITE
      }
    }
/** Provides image dimensions with extras support for KFrescoVitoDrawable. */
val imageDimensionsWithExtrasProvider =
    StringDebugDataProvider(
        "I",
        "Image dimensions",
        "The dimensions of the decoded image",
    ) { drawable, _ ->
      if (drawable is KFrescoVitoDrawable) {
        when (val model = drawable.actualImageLayer.getDataModel()) {
          null -> "unset"
          else -> formatDimensions(model.width, model.height)
        }
      } else {
        ""
      }
    }

/** Provides image origin with extras support for KFrescoVitoDrawable. */
val kFrescoImageOriginWithExtrasProvider =
    StringAndColorDebugDataProvider("o", "Origin", "The source of the image") { drawable, extras ->
      val originExtras = getOriginExtrasFromDrawable(drawable, extras)
      val origin = originExtras?.get("origin")?.toString() ?: "unknown"
      val color = DebugOverlayImageOriginColor.getImageOriginColor(origin)
      origin to color
    }

/** Creates an I:D fit ratio provider for KFrescoVitoDrawable. */
fun createImageFitRatioProvider(
    config: ImageFitRatioConfig,
): StringAndColorDebugDataProvider =
    StringAndColorDebugDataProvider(
        "I:D",
        "Image fit ratio",
        "The ratio of image dimensions to target dimensions",
    ) { drawable, _ ->
      if (drawable is KFrescoVitoDrawable) {
        val model = drawable.actualImageLayer.getDataModel()
        val viewport = drawable.viewportDimensions
        val targetWidth =
            if (viewport != null && viewport.width() > 0) viewport.width()
            else drawable.bounds.width()
        val targetHeight =
            if (viewport != null && viewport.height() > 0) viewport.height()
            else drawable.bounds.height()
        if (model != null) {
          computeImageFitRatioAndColor(model.width, model.height, targetWidth, targetHeight, config)
        } else {
          "" to Color.GRAY
        }
      } else {
        "" to Color.GRAY
      }
    }

/** Creates a wasted memory provider for KFrescoVitoDrawable. */
fun createWastedMemoryProvider(
    config: ImageFitRatioConfig,
): StringAndColorDebugDataProvider =
    StringAndColorDebugDataProvider(
        "waste",
        "Wasted memory",
        "Memory wasted by oversized image (image bytes - target bytes)",
    ) { drawable, _ ->
      if (drawable is KFrescoVitoDrawable) {
        val model = drawable.actualImageLayer.getDataModel()
        if (model is BitmapImageDataModel) {
          val viewport = drawable.viewportDimensions
          val targetWidth =
              if (viewport != null && viewport.width() > 0) viewport.width()
              else drawable.bounds.width()
          val targetHeight =
              if (viewport != null && viewport.height() > 0) viewport.height()
              else drawable.bounds.height()
          val bytesPerPixel =
              try {
                BitmapUtil.getPixelSizeForBitmapConfig(model.bitmap.config)
              } catch (_: UnsupportedOperationException) {
                BitmapUtil.ARGB_8888_BYTES_PER_PIXEL
              }
          computeWastedMemoryAndColor(
              model.width,
              model.height,
              targetWidth,
              targetHeight,
              bytesPerPixel,
              config,
          )
        } else {
          // Non-bitmap images (vector graphics, etc.) have no wasted memory
          "" to Color.GRAY
        }
      } else {
        "" to Color.GRAY
      }
    }

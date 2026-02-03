/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import android.graphics.Color
import com.facebook.fresco.ui.common.ControllerListener2.Extras
import com.facebook.fresco.vito.core.FrescoDrawableInterface
import com.facebook.fresco.vito.core.impl.debug.DebugOverlayImageOriginColor
import com.facebook.fresco.vito.core.impl.debug.StringAndColorDebugDataProvider
import com.facebook.fresco.vito.core.impl.debug.StringDebugDataProvider
import com.facebook.fresco.vito.core.impl.debug.formatDimensions
import com.facebook.fresco.vito.core.impl.debug.getOriginExtras

// ============================================================================
// KFrescoVitoDrawable-specific providers
// ============================================================================

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

private fun KFrescoVitoDrawable.extractOriginExtras(): Map<String, Any>? =
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
            drawable.extractOriginExtras()?.get("origin_sub")?.toString() ?: "unknown"

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

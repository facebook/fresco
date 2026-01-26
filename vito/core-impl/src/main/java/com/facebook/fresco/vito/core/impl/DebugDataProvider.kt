/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import android.graphics.Color
import com.facebook.fresco.vito.core.impl.debug.DebugOverlayImageOriginColor
import com.facebook.fresco.vito.core.impl.debug.StringAndColorDebugDataProvider
import com.facebook.fresco.vito.core.impl.debug.StringDebugDataProvider
import com.facebook.fresco.vito.core.impl.debug.formatDimensions

// ============================================================================
// KFrescoVitoDrawable-specific providers
// ============================================================================

/** Provides image dimensions for KFrescoVitoDrawable. */
val imageDimensionsProvider =
    StringDebugDataProvider("I", "Image dimensions", "The dimensions of the decoded image") {
      if (it is KFrescoVitoDrawable) {
        when (val model = it.actualImageLayer.getDataModel()) {
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

/** Provides image origin with color for KFrescoVitoDrawable. */
val imageOriginProvider =
    StringAndColorDebugDataProvider("o", "Origin", "The source of the image") {
      if (it is KFrescoVitoDrawable) {
        val origin = it.extractOriginExtras()?.get("origin")?.toString() ?: "unknown"
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
    ) {
      if (it is KFrescoVitoDrawable) {
        val originSubcategory = it.extractOriginExtras()?.get("origin_sub")?.toString() ?: "unknown"

        originSubcategory to Color.GRAY
      } else {
        "" to Color.WHITE
      }
    }

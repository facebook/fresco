/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import android.graphics.Color
import android.graphics.drawable.Drawable
import com.facebook.fresco.ui.common.VitoUtils
import com.facebook.fresco.vito.core.FrescoDrawableInterface
import com.facebook.fresco.vito.core.impl.debug.DebugOverlayImageOriginColor
import java.util.Locale

sealed class DebugDataProvider(
    val shortName: String,
    val longName: String,
    val description: String
)

class StringDebugDataProvider(
    shortName: String,
    longName: String,
    description: String,
    val extractData: (FrescoDrawableInterface) -> String
) : DebugDataProvider(shortName, longName, description)

val imageIDProvider =
    StringDebugDataProvider("id", "image ID", "The ID of the image") {
      VitoUtils.getStringId(it.imageId)
    }

val drawableDimensionsProvider =
    StringDebugDataProvider(
        "D", "Drawable dimensions", "The visible Drawable dimensions on screen") {
          if (it is Drawable) {
            formatDimensions(it.bounds.width(), it.bounds.height())
          } else {
            ""
          }
        }

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

fun formatDimensions(width: Int, height: Int): String =
    String.format(Locale.US, "%dx%d", width, height)

class StringAndColorDebugDataProvider(
    shortName: String,
    longName: String,
    description: String,
    val extractDataAndColor: (FrescoDrawableInterface) -> Pair<String, Int>
) : DebugDataProvider(shortName, longName, description)

private fun KFrescoVitoDrawable.extractOriginExtras(): Map<String, Any>? =
    dataSource?.extras
        ?:
        // We did not receive data source extras, so the image did not come from the image
        // pipeline but from the bitmap memory cache shortcut
        obtainExtras().shortcutExtras

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

val imageOriginSubcategoryProvider =
    StringAndColorDebugDataProvider(
        "o_s", "Origin Subcategory", "The subcategory of source of the image") {
          if (it is KFrescoVitoDrawable) {
            val originSubcategory =
                it.extractOriginExtras()?.get("origin_sub")?.toString() ?: "unknown"

            originSubcategory to Color.GRAY
          } else {
            "" to Color.WHITE
          }
        }

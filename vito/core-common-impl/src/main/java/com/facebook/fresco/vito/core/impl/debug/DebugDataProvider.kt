/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl.debug

import android.graphics.drawable.Drawable
import com.facebook.fresco.ui.common.VitoUtils
import com.facebook.fresco.vito.core.FrescoDrawableInterface
import java.util.Locale

/**
 * Base sealed class for debug data providers.
 *
 * Debug data providers are used to extract debug information from [FrescoDrawableInterface] for
 * display in debug overlays.
 */
sealed class DebugDataProvider(
    val shortName: String,
    val longName: String,
    val description: String,
)

/**
 * A debug data provider that extracts a string value from [FrescoDrawableInterface].
 *
 * Use this for simple string data that doesn't require extras (e.g., image ID, drawable
 * dimensions).
 */
class StringDebugDataProvider(
    shortName: String,
    longName: String,
    description: String,
    val extractData: (FrescoDrawableInterface) -> String,
) : DebugDataProvider(shortName, longName, description)

/**
 * A debug data provider that extracts a string value with associated color from
 * [FrescoDrawableInterface].
 *
 * Use this for data that should be displayed with a color indicator (e.g., image origin).
 */
class StringAndColorDebugDataProvider(
    shortName: String,
    longName: String,
    description: String,
    val extractDataAndColor: (FrescoDrawableInterface) -> Pair<String, Int>,
) : DebugDataProvider(shortName, longName, description)

// ============================================================================
// Utility functions
// ============================================================================

/** Formats width and height as a dimension string (e.g., "1920x1080"). */
fun formatDimensions(width: Int, height: Int): String =
    String.format(Locale.US, "%dx%d", width, height)

// ============================================================================
// Common providers that work with any FrescoDrawableInterface
// ============================================================================

/** Provides the image ID. */
val imageIDProvider: StringDebugDataProvider =
    StringDebugDataProvider("id", "image ID", "The ID of the image") {
      VitoUtils.getStringId(it.imageId)
    }

/**
 * Provides drawable dimensions (works with any FrescoDrawableInterface that is also a Drawable).
 */
val drawableDimensionsProvider: StringDebugDataProvider =
    StringDebugDataProvider(
        "D",
        "Drawable dimensions",
        "The visible Drawable dimensions on screen",
    ) {
      if (it is Drawable) {
        formatDimensions(it.bounds.width(), it.bounds.height())
      } else {
        ""
      }
    }

/** Provides HDR gainmap information. */
val hdrGainmapProvider: StringDebugDataProvider =
    StringDebugDataProvider(
        shortName = "hdr",
        longName = "hdrGainmap",
        description = "Has Bitmap with HDR Gainmap",
    ) { drawable ->
      drawable.hasBitmapWithGainmap().toString()
    }

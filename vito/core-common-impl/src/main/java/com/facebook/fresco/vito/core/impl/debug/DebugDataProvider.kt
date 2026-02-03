/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl.debug

import android.graphics.Color
import android.graphics.drawable.Drawable
import com.facebook.fresco.ui.common.ControllerListener2.Extras
import com.facebook.fresco.ui.common.VitoUtils
import com.facebook.fresco.vito.core.FrescoDrawableInterface
import java.util.Locale

/**
 * Base sealed class for debug data providers.
 *
 * Debug data providers are used to extract debug information from [FrescoDrawableInterface] for
 * display in debug overlays. All providers take an optional [Extras] parameter - providers that
 * don't need extras can simply ignore it.
 */
sealed class DebugDataProvider(
    val shortName: String,
    val longName: String,
    val description: String,
)

/** A debug data provider that extracts a string value. */
class StringDebugDataProvider(
    shortName: String,
    longName: String,
    description: String,
    val extractData: (FrescoDrawableInterface, Extras?) -> String,
) : DebugDataProvider(shortName, longName, description) {
  /** Convenience method for callers that don't have extras. */
  fun extractData(drawable: FrescoDrawableInterface): String = extractData(drawable, null)
}

/** A debug data provider that extracts a string value with associated color. */
class StringAndColorDebugDataProvider(
    shortName: String,
    longName: String,
    description: String,
    val extractDataAndColor: (FrescoDrawableInterface, Extras?) -> Pair<String, Int>,
) : DebugDataProvider(shortName, longName, description) {
  /** Convenience method for callers that don't have extras. */
  fun extractDataAndColor(drawable: FrescoDrawableInterface): Pair<String, Int> =
      extractDataAndColor(drawable, null)
}

// ============================================================================
// Utility functions
// ============================================================================

/** Formats width and height as a dimension string (e.g., "1920x1080"). */
fun formatDimensions(width: Int, height: Int): String =
    String.format(Locale.US, "%dx%d", width, height)

/** Abbreviates the origin string for compact display. */
fun abbreviateOrigin(origin: String): String =
    when (origin) {
      "memory_bitmap" -> "MEM"
      "memory_encoded" -> "ENC"
      "disk" -> "DISK"
      "network" -> "NET"
      "local" -> "LOCAL"
      else -> origin.uppercase(Locale.US).take(4)
    }

/** Truncates a URI to show only the last part of the path. */
fun truncateUri(uri: String, maxLength: Int = 20): String {
  val lastSlash = uri.lastIndexOf('/')
  val lastPart =
      if (lastSlash >= 0 && lastSlash < uri.length - 1) {
        uri.substring(lastSlash + 1)
      } else {
        uri
      }
  return if (lastPart.length > maxLength) {
    "..." + lastPart.takeLast(maxLength - 3)
  } else {
    lastPart
  }
}

/** Gets the origin extras from the provided Extras object. */
fun getOriginExtras(extras: Extras?): Map<String, Any>? {
  if (extras == null) return null
  return extras.datasourceExtras ?: extras.shortcutExtras
}

/** Gets the image format from extras. */
fun getImageFormat(extras: Extras?): String? {
  if (extras == null) return null

  // Try to get format from imageExtras first (normal path)
  extras.imageExtras?.get("image_format")?.toString()?.let {
    return it.uppercase(Locale.US).replace("_", "")
  }

  // For memory cache shortcut, try to get format from shortcutExtras
  extras.shortcutExtras?.get("image_format")?.toString()?.let {
    return it.uppercase(Locale.US).replace("_", "")
  }

  // Try datasourceExtras as fallback
  extras.datasourceExtras?.get("image_format")?.toString()?.let {
    return it.uppercase(Locale.US).replace("_", "")
  }

  return null
}

// ============================================================================
// Common providers that work with any FrescoDrawableInterface
// ============================================================================

/** Provides the image ID. */
val imageIDProvider: StringDebugDataProvider =
    StringDebugDataProvider("id", "image ID", "The ID of the image") { drawable, _ ->
      VitoUtils.getStringId(drawable.imageId)
    }

/**
 * Provides drawable dimensions (works with any FrescoDrawableInterface that is also a Drawable).
 */
val drawableDimensionsProvider: StringDebugDataProvider =
    StringDebugDataProvider(
        "D",
        "Drawable dimensions",
        "The visible Drawable dimensions on screen",
    ) { drawable, _ ->
      if (drawable is Drawable) {
        formatDimensions(drawable.bounds.width(), drawable.bounds.height())
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
    ) { drawable, _ ->
      drawable.hasBitmapWithGainmap().toString()
    }
// ============================================================================
// Extras-based providers
// ============================================================================

/** Provides drawable dimensions with extras support. */
val drawableDimensionsWithExtrasProvider: StringDebugDataProvider =
    StringDebugDataProvider(
        "D",
        "Drawable dimensions",
        "The visible Drawable dimensions on screen",
    ) { drawable, _ ->
      if (drawable is Drawable) {
        formatDimensions(drawable.bounds.width(), drawable.bounds.height())
      } else {
        ""
      }
    }

/** Provides image origin with color. */
val imageOriginWithExtrasProvider: StringAndColorDebugDataProvider =
    StringAndColorDebugDataProvider("o", "Origin", "The source of the image") { drawable, extras ->
      val originExtras =
          getOriginExtras(extras) ?: (drawable.extras as? Extras)?.let { getOriginExtras(it) }
      val origin = originExtras?.get("origin")?.toString() ?: "unknown"
      val color = DebugOverlayImageOriginColor.getImageOriginColor(origin)
      origin to color
    }

/** Provides abbreviated image origin with color (for compact display). */
val abbreviatedOriginWithExtrasProvider: StringAndColorDebugDataProvider =
    StringAndColorDebugDataProvider(
        "o",
        "Origin",
        "The source of the image (abbreviated)",
    ) { drawable, extras ->
      val originExtras =
          getOriginExtras(extras) ?: (drawable.extras as? Extras)?.let { getOriginExtras(it) }
      val origin = originExtras?.get("origin")?.toString() ?: "unknown"
      val color = DebugOverlayImageOriginColor.getImageOriginColor(origin)
      abbreviateOrigin(origin) to color
    }

/** Provides image origin subcategory with color. */
val imageOriginSubcategoryWithExtrasProvider: StringAndColorDebugDataProvider =
    StringAndColorDebugDataProvider(
        "o_s",
        "Origin Subcategory",
        "The subcategory of source of the image",
    ) { drawable, extras ->
      val originExtras =
          getOriginExtras(extras) ?: (drawable.extras as? Extras)?.let { getOriginExtras(it) }
      val originSubcategory = originExtras?.get("origin_sub")?.toString() ?: "unknown"
      originSubcategory to Color.GRAY
    }

/** Provides image format. */
val imageFormatProvider: StringDebugDataProvider =
    StringDebugDataProvider(
        "fmt",
        "Image Format",
        "The format of the image (JPEG, PNG, WEBP, etc.)",
    ) { drawable, extras ->
      // Try to get from extras first
      getImageFormat(extras)?.let {
        return@StringDebugDataProvider it
      }

      // Try to get from drawable's stored extras
      (drawable.extras as? Extras)?.let { getImageFormat(it) } ?: ""
    }

/** Provides truncated image URI. */
val imageUriProvider: StringDebugDataProvider =
    StringDebugDataProvider(
        "uri",
        "Image URI",
        "The URI of the image (truncated)",
    ) { drawable, _ ->
      val uri = drawable.imageRequest?.finalImageRequest?.sourceUri?.toString()
      if (uri != null) {
        truncateUri(uri)
      } else {
        ""
      }
    }

/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

@file:SuppressLint("ColorConstantUsageIssue")

package com.facebook.fresco.vito.core.impl.debug

import android.annotation.SuppressLint
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

/**
 * Configuration for the I:D fit ratio visualization.
 *
 * The debug overlay shows the image fit ratio with color-coded background tinting. This is
 * automatically enabled in debug builds when the debug overlay is active.
 *
 * @param greenMinRatio minimum ratio for green (perfect fit). Default 0.9
 * @param greenMaxRatio maximum ratio for green (perfect fit). Default 1.1
 * @param yellowMaxRatio maximum ratio for yellow (slightly oversized). Default 1.5. Ratios below
 *   [greenMinRatio] or above [yellowMaxRatio] are red.
 * @param wastedMemoryRedThresholdBytes wasted memory threshold in bytes above which the overlay
 *   turns red. Default 1 MiB.
 */
data class ImageFitRatioConfig(
    val greenMinRatio: Float = 0.9f,
    val greenMaxRatio: Float = 1.1f,
    val yellowMaxRatio: Float = 1.5f,
    val wastedMemoryRedThresholdBytes: Long = 1L * 1024 * 1024,
)

/**
 * Computes the I:D fit ratio and returns a formatted string with a color indicating the fit
 * quality.
 *
 * The ratio is computed as max(imageWidth/drawableWidth, imageHeight/drawableHeight) to capture the
 * worst-case dimension.
 *
 * Color coding (using thresholds from [config]):
 * - Green: greenMinRatio <= ratio <= greenMaxRatio (perfect fit)
 * - Yellow: greenMaxRatio < ratio <= yellowMaxRatio (slightly oversized)
 * - Red: ratio < greenMinRatio (undersized) or ratio > yellowMaxRatio (oversized)
 */
fun computeImageFitRatioAndColor(
    imageWidth: Int,
    imageHeight: Int,
    drawableWidth: Int,
    drawableHeight: Int,
    config: ImageFitRatioConfig = ImageFitRatioConfig(),
): Pair<String, Int> {
  if (imageWidth <= 0 || imageHeight <= 0 || drawableWidth <= 0 || drawableHeight <= 0) {
    return "" to Color.GRAY
  }
  val widthRatio = imageWidth.toFloat() / drawableWidth
  val heightRatio = imageHeight.toFloat() / drawableHeight
  val ratio = maxOf(widthRatio, heightRatio)
  val formatted = String.format(Locale.US, "%.2f", ratio)
  val color =
      when {
        ratio < config.greenMinRatio -> Color.RED
        ratio <= config.greenMaxRatio -> Color.GREEN
        ratio <= config.yellowMaxRatio -> Color.YELLOW
        else -> Color.RED
      }
  return formatted to color
}

/** Default bytes per pixel, assuming ARGB_8888 bitmap config. */
const val DEFAULT_BYTES_PER_PIXEL = 4

/**
 * Computes the wasted memory from an oversized image and returns a formatted string with a color.
 *
 * Memory usage per image = width * height * bytesPerPixel. Wasted memory is the difference between
 * the image's memory footprint and what would be needed for the target dimensions.
 *
 * Color coding:
 * - Red: wasted > (default 1 MiB)
 * - Green: wasted > 0 but within threshold
 * - Gray: no waste or invalid dimensions
 */
fun computeWastedMemoryAndColor(
    imageWidth: Int,
    imageHeight: Int,
    drawableWidth: Int,
    drawableHeight: Int,
    bytesPerPixel: Int = DEFAULT_BYTES_PER_PIXEL,
    config: ImageFitRatioConfig = ImageFitRatioConfig(),
): Pair<String, Int> {
  // bytesPerPixel <= 0 covers vector graphics and other non-raster images
  if (
      imageWidth <= 0 ||
          imageHeight <= 0 ||
          drawableWidth <= 0 ||
          drawableHeight <= 0 ||
          bytesPerPixel <= 0
  ) {
    return "" to Color.GRAY
  }
  val imageBytes = imageWidth.toLong() * imageHeight * bytesPerPixel
  val targetBytes = drawableWidth.toLong() * drawableHeight * bytesPerPixel
  val wastedBytes = maxOf(0L, imageBytes - targetBytes)
  if (wastedBytes == 0L) {
    return "0 B" to Color.GREEN
  }
  val formatted =
      when {
        wastedBytes >= 1024L * 1024 ->
            String.format(Locale.US, "%.1f MiB", wastedBytes / (1024.0 * 1024))
        wastedBytes >= 1024L -> String.format(Locale.US, "%.1f KiB", wastedBytes / 1024.0)
        else -> "$wastedBytes B"
      }
  val color = if (wastedBytes > config.wastedMemoryRedThresholdBytes) Color.RED else Color.GREEN
  return formatted to color
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

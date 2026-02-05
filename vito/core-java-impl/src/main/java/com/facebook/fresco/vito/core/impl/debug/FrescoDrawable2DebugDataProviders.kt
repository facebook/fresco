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
import com.facebook.fresco.ui.common.ControllerListener2.Extras
import com.facebook.fresco.vito.core.impl.FrescoDrawable2

// ============================================================================
// FrescoDrawable2-specific providers
// ============================================================================

/**
 * Provides actual image dimensions for FrescoDrawable2.
 *
 * This returns the dimensions of the decoded image, which may differ from the drawable dimensions
 * on screen.
 */
val actualImageDimensionsProvider: StringDebugDataProvider =
    StringDebugDataProvider(
        "I",
        "Image dimensions",
        "The dimensions of the decoded image",
    ) { drawable, _ ->
      if (drawable is FrescoDrawable2) {
        val width = drawable.actualImageWidthPx
        val height = drawable.actualImageHeightPx
        if (width > 0 && height > 0) {
          formatDimensions(width, height)
        } else {
          "unset"
        }
      } else {
        ""
      }
    }

/**
 * Provides actual image dimensions with "img:" prefix for FrescoDrawable2.
 *
 * This is useful when displaying alongside drawable dimensions to distinguish them.
 */
val actualImageDimensionsWithPrefixProvider: StringDebugDataProvider =
    StringDebugDataProvider(
        "img",
        "Image dimensions",
        "The dimensions of the decoded image (with prefix)",
    ) { drawable, _ ->
      if (drawable is FrescoDrawable2) {
        val width = drawable.actualImageWidthPx
        val height = drawable.actualImageHeightPx
        if (width > 0 && height > 0) {
          "img:${formatDimensions(width, height)}"
        } else {
          ""
        }
      } else {
        ""
      }
    }

/**
 * Provides scale type for FrescoDrawable2.
 *
 * Returns the scale type used to display the image (e.g., CENTER_CROP, FIT_CENTER).
 */
val scaleTypeProvider: StringDebugDataProvider =
    StringDebugDataProvider(
        "scale",
        "Scale Type",
        "The scale type used to display the image",
    ) { drawable, _ ->
      if (drawable is FrescoDrawable2) {
        drawable.actualImageScaleType?.toString() ?: ""
      } else {
        ""
      }
    }

/**
 * Provides abbreviated scale type for FrescoDrawable2.
 *
 * Returns the first 6 characters of the scale type for compact display.
 */
val abbreviatedScaleTypeProvider: StringDebugDataProvider =
    StringDebugDataProvider(
        "scale",
        "Scale Type",
        "The scale type used to display the image (abbreviated)",
    ) { drawable, _ ->
      if (drawable is FrescoDrawable2) {
        drawable.actualImageScaleType?.toString()?.take(6) ?: ""
      } else {
        ""
      }
    }

// ============================================================================
// Extras-based providers for FrescoDrawable2
// ============================================================================

/** Helper to extract origin extras from Extras. */
private fun getOriginExtras(extras: Extras?): Map<String, Any>? =
    extras?.datasourceExtras ?: extras?.shortcutExtras

/** Provides abbreviated image origin with color (using Extras). */
val abbreviatedOriginWithExtrasProvider: StringAndColorDebugDataProvider =
    StringAndColorDebugDataProvider(
        "o",
        "Origin",
        "The source of the image (abbreviated)",
    ) { _, extras ->
      val originExtras = getOriginExtras(extras)
      val origin = originExtras?.get("origin")?.toString() ?: "unknown"
      val abbreviated = origin.take(3).uppercase()
      val color = DebugOverlayImageOriginColor.getImageOriginColor(origin)
      abbreviated to color
    }

/** Provides drawable dimensions (using the common provider but formatted for Extras usage). */
val drawableDimensionsWithExtrasProvider: StringDebugDataProvider = drawableDimensionsProvider

/** Provides image format from Extras. */
val imageFormatProvider: StringDebugDataProvider =
    StringDebugDataProvider(
        "fmt",
        "Image Format",
        "The format of the image (JPEG, PNG, WEBP, etc.)",
    ) { _, extras ->
      val originExtras = getOriginExtras(extras)
      originExtras?.get("encodedImageFormat")?.toString() ?: ""
    }

/** Provides truncated image URI from Extras. */
val imageUriProvider: StringDebugDataProvider =
    StringDebugDataProvider(
        "uri",
        "Image URI",
        "The URI of the image (truncated)",
    ) { _, extras ->
      val uri = extras?.imageSourceExtras?.get("uri")?.toString() ?: ""
      if (uri.length > 30) {
        "..." + uri.takeLast(27)
      } else {
        uri
      }
    }

/** Provides full image origin with color (using Extras). */
val imageOriginWithExtrasProvider: StringAndColorDebugDataProvider =
    StringAndColorDebugDataProvider(
        "origin",
        "Origin",
        "The source of the image",
    ) { _, extras ->
      val originExtras = getOriginExtras(extras)
      val origin = originExtras?.get("origin")?.toString() ?: "unknown"
      val color = DebugOverlayImageOriginColor.getImageOriginColor(origin)
      origin to color
    }

/** Provides image origin subcategory with color (using Extras). */
val imageOriginSubcategoryWithExtrasProvider: StringAndColorDebugDataProvider =
    StringAndColorDebugDataProvider(
        "origin_sub",
        "Origin Subcategory",
        "The subcategory of source of the image",
    ) { _, extras ->
      val originExtras = getOriginExtras(extras)
      val originSubcategory = originExtras?.get("origin_sub")?.toString() ?: "unknown"
      originSubcategory to Color.GRAY
    }

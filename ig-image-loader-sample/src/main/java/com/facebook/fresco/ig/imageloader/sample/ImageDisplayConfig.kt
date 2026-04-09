/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.ig.imageloader.sample

import com.instagram.fresco.vitoimagetype.ImageType

/** Shape of the image view used to display the image. */
enum class ImageShape {
  RECTANGLE,
  CIRCULAR,
  ROUNDED,
}

/** Which view type this preset belongs to. */
enum class ViewType(val label: String) {
  IG_IMAGE_VIEW("IgImageView"),
  CIRCULAR_IMAGE_VIEW("CircularImageView"),
  ROUNDED_CORNER_IMAGE_VIEW("RoundedCornerImageView"),
}

/**
 * Describes a preset configuration for loading an image. Each preset maps to a real usage pattern
 * in the Instagram Android app. Engineers can select a preset to pre-fill the config form, then
 * override individual options.
 */
data class ImageDisplayConfig(
    val label: String,
    val surface: String,
    val viewType: ViewType,
    val imageType: ImageType,
    val imageUrl: String,
    val maxSampleSize: Int = 1,
    val lowFidelity: Boolean = false,
    val progressive: Boolean = false,
    val shape: ImageShape = ImageShape.RECTANGLE,
    val cornerRadiusDp: Int = 0,
    val strokeWidthDp: Int = 0,
    val strokeColor: Int = 0,
) {
  companion object {
    private const val DEFAULT_IMAGE_URL = "https://www.facebook.com/images/fb_icon_325x325.png"

    val IG_IMAGE_VIEW_PRESETS =
        listOf(
            ImageDisplayConfig(
                label = "Feed Photo",
                surface = "Feed timeline",
                viewType = ViewType.IG_IMAGE_VIEW,
                imageType = ImageType.UNIDENTIFIED,
                imageUrl = DEFAULT_IMAGE_URL,
                progressive = true,
            ),
            ImageDisplayConfig(
                label = "Grid Thumbnail",
                surface = "Discovery grid, profile grid",
                viewType = ViewType.IG_IMAGE_VIEW,
                imageType = ImageType.GRID,
                imageUrl = DEFAULT_IMAGE_URL,
                maxSampleSize = 4,
                lowFidelity = true,
            ),
            ImageDisplayConfig(
                label = "Grid (Aggressive)",
                surface = "Explore, small previews",
                viewType = ViewType.IG_IMAGE_VIEW,
                imageType = ImageType.GRID,
                imageUrl = DEFAULT_IMAGE_URL,
                maxSampleSize = 8,
                lowFidelity = true,
            ),
            ImageDisplayConfig(
                label = "Video Cover",
                surface = "Reels, IGTV, Stories",
                viewType = ViewType.IG_IMAGE_VIEW,
                imageType = ImageType.VIDEO_COVER,
                imageUrl = DEFAULT_IMAGE_URL,
                maxSampleSize = 2,
                progressive = true,
            ),
            ImageDisplayConfig(
                label = "Ads Standard",
                surface = "Ads feed, carousel",
                viewType = ViewType.IG_IMAGE_VIEW,
                imageType = ImageType.ADS_NON_9_16,
                imageUrl = DEFAULT_IMAGE_URL,
            ),
            ImageDisplayConfig(
                label = "Ads IAB",
                surface = "IAB standard ad format",
                viewType = ViewType.IG_IMAGE_VIEW,
                imageType = ImageType.ADS_IAB_SCREENSHOT,
                imageUrl = DEFAULT_IMAGE_URL,
            ),
        )

    val CIRCULAR_IMAGE_VIEW_PRESETS =
        listOf(
            ImageDisplayConfig(
                label = "Profile Avatar",
                surface = "Profile header, mentions, DMs",
                viewType = ViewType.CIRCULAR_IMAGE_VIEW,
                imageType = ImageType.PROFILE_CIRCULAR,
                imageUrl = DEFAULT_IMAGE_URL,
                shape = ImageShape.CIRCULAR,
            ),
            ImageDisplayConfig(
                label = "Avatar (Bordered)",
                surface = "Story ring, live badge",
                viewType = ViewType.CIRCULAR_IMAGE_VIEW,
                imageType = ImageType.PROFILE_CIRCULAR,
                imageUrl = DEFAULT_IMAGE_URL,
                shape = ImageShape.CIRCULAR,
                strokeWidthDp = 3,
                strokeColor = 0xFFE1306C.toInt(),
            ),
        )

    val ROUNDED_CORNER_IMAGE_VIEW_PRESETS =
        listOf(
            ImageDisplayConfig(
                label = "Story Preview",
                surface = "Story tray, reel tray",
                viewType = ViewType.ROUNDED_CORNER_IMAGE_VIEW,
                imageType = ImageType.VIDEO_COVER,
                imageUrl = DEFAULT_IMAGE_URL,
                shape = ImageShape.ROUNDED,
                cornerRadiusDp = 8,
                progressive = true,
            ),
            ImageDisplayConfig(
                label = "Rounded Card",
                surface = "Saved collections, cards",
                viewType = ViewType.ROUNDED_CORNER_IMAGE_VIEW,
                imageType = ImageType.UNIDENTIFIED,
                imageUrl = DEFAULT_IMAGE_URL,
                shape = ImageShape.ROUNDED,
                cornerRadiusDp = 12,
            ),
        )

    fun presetsForViewType(viewType: ViewType): List<ImageDisplayConfig> =
        when (viewType) {
          ViewType.IG_IMAGE_VIEW -> IG_IMAGE_VIEW_PRESETS
          ViewType.CIRCULAR_IMAGE_VIEW -> CIRCULAR_IMAGE_VIEW_PRESETS
          ViewType.ROUNDED_CORNER_IMAGE_VIEW -> ROUNDED_CORNER_IMAGE_VIEW_PRESETS
        }
  }
}

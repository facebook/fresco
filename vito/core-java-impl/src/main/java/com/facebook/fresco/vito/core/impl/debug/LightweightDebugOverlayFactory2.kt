/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl.debug

import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import com.facebook.common.internal.Supplier
import com.facebook.fresco.ui.common.ControllerListener2.Extras
import com.facebook.fresco.vito.core.impl.FrescoDrawable2

/**
 * Configuration for the lightweight debug overlay.
 *
 * @param showOrigin Show the image origin (MEM, DISK, NET, etc.)
 * @param showDimensions Show the drawable dimensions
 * @param showImageDimensions Show the actual image dimensions
 * @param showFormat Show the image format (JPEG, PNG, WEBP, etc.)
 * @param showUri Show the image URI (truncated)
 * @param showScaleType Show the scale type
 */
data class LightweightDebugOverlayConfig(
    val showOrigin: Boolean = true,
    val showDimensions: Boolean = true,
    val showImageDimensions: Boolean = true,
    val showFormat: Boolean = false,
    val showUri: Boolean = false,
    val showScaleType: Boolean = false,
)

/**
 * A lightweight debug overlay factory that shows a small color-coded bar at the bottom of images.
 *
 * The bar color indicates the image origin:
 * - Green: memory_bitmap or local (fastest)
 * - Yellow: disk or memory_encoded (medium)
 * - Red: network (slowest)
 * - Gray: unknown
 *
 * The bar displays configurable information based on the provided config or list of providers.
 *
 * This factory uses the shared [DebugDataProvider] infrastructure to extract debug data, ensuring
 * consistency with other debug overlays in the Fresco library.
 */
class LightweightDebugOverlayFactory2(
    private val debugOverlayEnabled: Supplier<Boolean>,
    private val config: LightweightDebugOverlayConfig = LightweightDebugOverlayConfig(),
    private val colorProvider: StringAndColorDebugDataProvider =
        abbreviatedOriginWithExtrasProvider,
    private val debugDataProviders: List<DebugDataProvider>? = null,
) : DebugOverlayFactory2 {

  override fun update(drawable: FrescoDrawable2, extras: Extras?) {
    if (!debugOverlayEnabled.get()) {
      // Clear callback on existing overlay to prevent stale callbacks
      val existingOverlay = drawable.overlayDrawable
      when (existingOverlay) {
        is LightweightDebugOverlayDrawable -> existingOverlay.onBoundsChangedCallback = null
        is LightweightDebugOverlayDrawableWrapper ->
            existingOverlay.debugOverlayDrawable.onBoundsChangedCallback = null
      }
      return
    }

    val overlay = extractOrCreate(drawable)
    // Store current bounds to detect actual changes and prevent callback loops
    val previousBounds = Rect(overlay.bounds)

    overlay.reset()
    overlay.onBoundsChangedCallback = { newBounds: Rect? ->
      if (newBounds != null && newBounds != previousBounds) {
        update(drawable, extras)
        previousBounds.set(newBounds)
      }
    }
    setData(overlay, drawable, extras)
    overlay.invalidateSelf()
  }

  private fun setData(
      overlay: LightweightDebugOverlayDrawable,
      drawable: FrescoDrawable2,
      extras: Extras?,
  ) {
    // Get origin info and set bar color using the color provider
    val (_, originColor) = colorProvider.extractDataAndColor(drawable, extras)
    overlay.setOriginColor(originColor)

    // Build compact debug text
    val debugText =
        if (debugDataProviders != null) {
          buildDebugTextFromProviders(drawable, extras)
        } else {
          buildDebugTextFromConfig(drawable, extras)
        }
    overlay.setDebugText(debugText)
  }

  /**
   * Extracts text from a [DebugDataProvider] using the appropriate method based on provider type.
   */
  private fun extractText(
      provider: DebugDataProvider,
      drawable: FrescoDrawable2,
      extras: Extras?,
  ): String {
    return when (provider) {
      is StringDebugDataProvider -> provider.extractData(drawable, extras)
      is StringAndColorDebugDataProvider -> provider.extractDataAndColor(drawable, extras).first
    }
  }

  /** Builds debug text by iterating through the configured list of [DebugDataProvider]s. */
  private fun buildDebugTextFromProviders(drawable: FrescoDrawable2, extras: Extras?): String {
    val parts = mutableListOf<String>()

    debugDataProviders?.forEach { provider ->
      val text = extractText(provider, drawable, extras)
      if (text.isNotEmpty()) {
        parts.add(text)
      }
    }

    return parts.joinToString(" | ")
  }

  /** Builds debug text based on the [LightweightDebugOverlayConfig]. */
  private fun buildDebugTextFromConfig(drawable: FrescoDrawable2, extras: Extras?): String {
    val parts = mutableListOf<String>()

    // Add origin (abbreviated) using the shared provider
    if (config.showOrigin) {
      val text = extractText(abbreviatedOriginWithExtrasProvider, drawable, extras)
      if (text.isNotEmpty()) {
        parts.add(text)
      }
    }

    // Add drawable dimensions using the shared provider
    if (config.showDimensions) {
      val text = extractText(drawableDimensionsWithExtrasProvider, drawable, extras)
      if (text.isNotEmpty()) {
        parts.add(text)
      }
    }

    // Add image dimensions using the provider
    // Only show if showDimensions is false OR if image dimensions differ from drawable dimensions
    if (config.showImageDimensions) {
      val imageDimensionsText =
          extractText(actualImageDimensionsWithPrefixProvider, drawable, extras)
      if (imageDimensionsText.isNotEmpty()) {
        val bounds = drawable.bounds
        val imageWidth = drawable.actualImageWidthPx
        val imageHeight = drawable.actualImageHeightPx
        parts.add(imageDimensionsText)
      }
    }

    // Add image format using the shared provider
    if (config.showFormat) {
      val text = extractText(imageFormatProvider, drawable, extras)
      if (text.isNotEmpty()) {
        parts.add(text)
      }
    }

    // Add scale type using the provider
    if (config.showScaleType) {
      val text = extractText(abbreviatedScaleTypeProvider, drawable, extras)
      if (text.isNotEmpty()) {
        parts.add(text)
      }
    }

    // Add URI (truncated) using the shared provider
    if (config.showUri) {
      val text = extractText(imageUriProvider, drawable, extras)
      if (text.isNotEmpty()) {
        parts.add(text)
      }
    }

    return parts.joinToString(" | ")
  }

  private class LightweightDebugOverlayDrawableWrapper(
      existingOverlayDrawable: Drawable,
      val debugOverlayDrawable: LightweightDebugOverlayDrawable,
  ) : LayerDrawable(arrayOf(existingOverlayDrawable, debugOverlayDrawable))

  companion object {
    private fun extractOrCreate(drawable: FrescoDrawable2): LightweightDebugOverlayDrawable {
      val existingOverlay = drawable.overlayDrawable
      if (existingOverlay is LightweightDebugOverlayDrawable) {
        return existingOverlay
      } else if (existingOverlay is LightweightDebugOverlayDrawableWrapper) {
        return existingOverlay.debugOverlayDrawable
      }
      val debugOverlay = LightweightDebugOverlayDrawable()
      if (existingOverlay != null) {
        drawable.setOverlayDrawable(
            LightweightDebugOverlayDrawableWrapper(existingOverlay, debugOverlay)
        )
      } else {
        drawable.setOverlayDrawable(debugOverlay)
      }
      return debugOverlay
    }
  }
}

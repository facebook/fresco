/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl.debug

import android.graphics.Color
import com.facebook.common.internal.Supplier
import com.facebook.fresco.ui.common.ControllerListener2.Extras
import com.facebook.fresco.ui.common.VitoUtils
import com.facebook.fresco.vito.core.FrescoDrawableInterface
import com.facebook.fresco.vito.core.VitoImageRequest
import com.facebook.fresco.vito.core.impl.FrescoDrawable2
import java.util.Locale

open class DefaultDebugOverlayFactory2(
    var showExtendedInformation: Boolean = true,
    private var showExtendedImageSourceExtraInformation: Boolean = false,
    debugOverlayEnabled: Supplier<Boolean>,
) : BaseDebugOverlayFactory2(debugOverlayEnabled) {

  fun setShowExtendedImageSourceExtraInformation(showExtendedImageSourceExtraInformation: Boolean) {
    this.showExtendedImageSourceExtraInformation = showExtendedImageSourceExtraInformation
  }

  override fun setData(
      overlay: DebugOverlayDrawable,
      drawable: FrescoDrawableInterface,
      extras: Extras?
  ) {
    setBasicData(overlay, drawable)
    setImageRequestData(overlay, drawable.imageRequest)
    setImageOriginData(overlay, extras)
    setImageSourceExtra(overlay, extras)
  }

  private fun setBasicData(overlay: DebugOverlayDrawable, drawable: FrescoDrawableInterface) {
    overlay.drawIdentifier = showExtendedInformation
    val tag = if (showExtendedInformation) "ID" else overlay.identifier
    overlay.addDebugData(tag, VitoUtils.getStringId(drawable.imageId))

    val abstractDrawable = drawable as? FrescoDrawable2 ?: return
    val bounds = abstractDrawable.bounds
    overlay.addDebugData("D", formatDimensions(bounds.width(), bounds.height()))
    if (showExtendedInformation) {
      overlay.addDebugData("DAR", (bounds.width() / bounds.height().toFloat()).toString())
    }
    overlay.addDebugData(
        "I",
        formatDimensions(abstractDrawable.actualImageWidthPx, abstractDrawable.actualImageHeightPx))
    if (showExtendedInformation && abstractDrawable.actualImageHeightPx > 0) {
      overlay.addDebugData(
          "IAR",
          (abstractDrawable.actualImageWidthPx / abstractDrawable.actualImageHeightPx.toFloat())
              .toString())
    }
    val focusPoint = abstractDrawable.actualImageFocusPoint
    if (focusPoint != null) {
      overlay.addDebugData("FocusPointX", focusPoint.x.toString())
      overlay.addDebugData("FocusPointY", focusPoint.y.toString())
    }
  }

  private fun setImageOriginData(overlay: DebugOverlayDrawable, extras: Extras?) {
    var origin = "unknown"
    var originSubcategory = "unknown"
    if (extras != null) {
      var originExtras = extras.datasourceExtras
      if (originExtras == null) {
        // We did not receive data source extras, so the image did not come from the image pipeline
        // but from the bitmap memory cache shortcut
        originExtras = extras.shortcutExtras
      }
      if (originExtras != null) {
        origin = originExtras["origin"].toString()
        originSubcategory = originExtras["origin_sub"].toString()
      }
    }
    if (showExtendedInformation) {
      overlay.addDebugData(
          "origin", origin, DebugOverlayImageOriginColor.getImageOriginColor(origin))
      overlay.addDebugData("origin_sub", originSubcategory, Color.GRAY)
    } else {
      overlay.addDebugData(
          "o",
          "$origin | $originSubcategory",
          DebugOverlayImageOriginColor.getImageOriginColor(origin))
    }
  }

  private fun setImageSourceExtra(overlay: DebugOverlayDrawable, extras: Extras?) {
    if (showExtendedImageSourceExtraInformation && extras != null) {
      val sourceExtras = extras.imageSourceExtras ?: return
      for ((key, value) in sourceExtras) {
        overlay.addDebugData(key, value.toString())
      }
    }
  }

  private fun setImageRequestData(overlay: DebugOverlayDrawable, imageRequest: VitoImageRequest?) {
    if (imageRequest == null || !showExtendedInformation) {
      return
    }
    overlay.addDebugData("scale", imageRequest.imageOptions.actualImageScaleType.toString())
  }

  companion object {
    @JvmStatic
    protected fun formatDimensions(width: Int, height: Int): String =
        String.format(Locale.US, "%dx%d", width, height)
  }
}

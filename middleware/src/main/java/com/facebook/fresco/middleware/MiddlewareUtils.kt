/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.middleware

import android.graphics.PointF
import android.graphics.Rect
import android.net.Uri
import com.facebook.fresco.ui.common.ControllerListener2.Extras

object MiddlewareUtils {
  @JvmStatic
  fun obtainExtras(
      componentAttribution: Map<String, Any>,
      shortcutAttribution: Map<String, Any>,
      dataSourceExtras: Map<String, Any>?,
      imageSourceExtras: Map<String, Any>?,
      viewportDimensions: Rect?,
      scaleType: String?,
      focusPoint: PointF?,
      imageExtras: Map<String, Any>?,
      callerContext: Any?,
      logWithHighSamplingRate: Boolean = false,
      mainUri: Uri?,
  ): Extras {
    val extras = Extras()
    if (viewportDimensions != null) {
      extras.viewportWidth = viewportDimensions.width()
      extras.viewportHeight = viewportDimensions.height()
    }
    extras.scaleType = scaleType
    if (focusPoint != null) {
      extras.focusX = focusPoint.x
      extras.focusY = focusPoint.y
    }
    extras.callerContext = callerContext
    extras.logWithHighSamplingRate = logWithHighSamplingRate
    extras.mainUri = mainUri
    extras.datasourceExtras = dataSourceExtras
    extras.imageExtras = imageExtras
    extras.shortcutExtras = shortcutAttribution
    extras.componentExtras = componentAttribution
    extras.imageSourceExtras = imageSourceExtras
    return extras
  }
}

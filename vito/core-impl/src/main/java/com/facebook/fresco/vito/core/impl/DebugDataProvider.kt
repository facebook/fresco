/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import android.graphics.drawable.Drawable
import com.facebook.fresco.ui.common.VitoUtils
import com.facebook.fresco.vito.core.FrescoDrawableInterface
import java.util.Locale

class DebugDataProvider(
    val shortName: String,
    val longName: String,
    val description: String,
    val extractData: (FrescoDrawableInterface) -> String
)

val imageIDProvider =
    DebugDataProvider("id", "image ID", "The ID of the image") { VitoUtils.getStringId(it.imageId) }

val drawableDimensionsProvider =
    DebugDataProvider("D", "Drawable dimensions", "The visible Drawable dimensions on screen") {
      if (it is Drawable) {
        formatDimensions(it.bounds.width(), it.bounds.height())
      } else {
        ""
      }
    }

val imageDimensionsProvider =
    DebugDataProvider("I", "Image dimensions", "The dimensions of the decoded image") {
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

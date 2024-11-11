/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.compose

import android.widget.ImageView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.fresco.vito.source.ImageSource
import com.facebook.fresco.vito.view.VitoView

/** Fresco Image Composables */
@Composable
fun VitoImage(
    modifier: Modifier = Modifier,
    source: ImageSource,
    options: ImageOptions = ImageOptions.defaults(),
    callerContext: Any,
) {
  AndroidView(
      modifier = modifier,
      factory = { ImageView(it) },
      update = {
        VitoView.show(
            imageSource = source,
            target = it,
            imageOptions = options,
            callerContext = callerContext,
        )
      },
      onRelease = { VitoView.release(it) },
  )
}

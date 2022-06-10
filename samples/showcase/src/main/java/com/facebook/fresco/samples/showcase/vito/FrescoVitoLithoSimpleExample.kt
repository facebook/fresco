/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.vito

import com.facebook.fresco.samples.showcase.LithoSample
import com.facebook.fresco.samples.showcase.R
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider
import com.facebook.fresco.vito.litho.FrescoVitoImage2
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.fresco.vito.options.RoundingOptions
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext

/** Simple experimental Fresco Vito fragment that just displays an image. */
object FrescoVitoLithoSimpleExample : LithoSample {

  private val IMAGE_OPTIONS =
      ImageOptions.create()
          .placeholderRes(R.drawable.logo)
          .round(RoundingOptions.asCircle())
          .fadeDurationMs(3000)
          .build()

  override fun createLithoComponent(
      c: ComponentContext,
      uris: ImageUriProvider,
      callerContext: Any
  ): Component {
    val uri = uris.createSampleUri()
    return FrescoVitoImage2.create(c).uri(uri).imageOptions(IMAGE_OPTIONS).build()
  }
}

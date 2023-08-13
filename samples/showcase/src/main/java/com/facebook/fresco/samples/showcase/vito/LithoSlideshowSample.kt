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
import com.facebook.fresco.vito.litho.slideshow.FrescoVitoSlideshowComponent
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.fresco.vito.options.RoundingOptions
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext

class LithoSlideshowSample : LithoSample {
  private val imageOptions =
      ImageOptions.create()
          .placeholderRes(R.drawable.logo)
          .round(RoundingOptions.asCircle())
          .fadeDurationMs(0)
          .build()

  override fun createLithoComponent(
      c: ComponentContext,
      uris: ImageUriProvider,
      callerContext: Any
  ): Component {
    return FrescoVitoSlideshowComponent.create(c)
        .fadeTransitionMs(1000)
        .photoTransitionMs(4000)
        .uris(uris.getRandomSampleUris(ImageUriProvider.ImageSize.M, 5))
        .imageOptions(imageOptions)
        .callerContext(callerContext)
        .build()
  }
}

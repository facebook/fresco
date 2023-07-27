/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.vito

import android.graphics.Color
import androidx.core.content.ContextCompat
import com.facebook.fresco.samples.showcase.LithoSample
import com.facebook.fresco.samples.showcase.R
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider
import com.facebook.fresco.vito.litho.FrescoVitoImage2
import com.facebook.fresco.vito.options.BorderOptions
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.fresco.vito.source.DrawableImageSource
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext

/** Simple Fresco Vito example that loads a Drawable image via DrawableImageSourc. */
object FrescoVitoLithoDrawableImageSourceExample : LithoSample {

  private val IMAGE_OPTIONS =
      ImageOptions.create()
          .placeholderColor(Color.RED)
          .borders(BorderOptions.create(Color.BLUE, 8f))
          .build()

  override fun createLithoComponent(
      c: ComponentContext,
      uris: ImageUriProvider,
      callerContext: Any
  ): Component =
      FrescoVitoImage2.create(c)
          .imageSource(
              DrawableImageSource(ContextCompat.getDrawable(c.androidContext, R.drawable.logo)!!))
          .imageOptions(IMAGE_OPTIONS)
          .callerContext("FrescoVitoLithoDrawableImageSourceExample")
          .build()
}

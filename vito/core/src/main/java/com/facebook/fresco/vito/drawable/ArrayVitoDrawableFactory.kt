/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.drawable

import android.content.res.Resources
import android.graphics.drawable.Drawable
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.fresco.vito.options.ImageOptionsDrawableFactory
import com.facebook.imagepipeline.image.CloseableImage

class ArrayVitoDrawableFactory(private vararg val drawableFactories: ImageOptionsDrawableFactory) :
    ImageOptionsDrawableFactory {

  override fun createDrawable(
      resources: Resources,
      closeableImage: CloseableImage,
      imageOptions: ImageOptions
  ): Drawable? =
      drawableFactories.firstNotNullOfOrNull { factory ->
        factory.createDrawable(resources, closeableImage, imageOptions)
      }
}

/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.draweesupport

import android.content.res.Resources
import android.graphics.drawable.Drawable
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.fresco.vito.options.ImageOptionsDrawableFactory
import com.facebook.imagepipeline.drawable.DrawableFactory
import com.facebook.imagepipeline.image.CloseableImage

class DrawableFactoryWrapper private constructor(private val drawableFactory: DrawableFactory) :
    ImageOptionsDrawableFactory {
  override fun createDrawable(
      resources: Resources,
      closeableImage: CloseableImage,
      imageOptions: ImageOptions
  ): Drawable? =
      if (drawableFactory.supportsImageType(closeableImage)) {
        if (drawableFactory is ImageOptionsDrawableFactory) {
          drawableFactory.createDrawable(resources, closeableImage, imageOptions)
        } else {
          drawableFactory.createDrawable(closeableImage)
        }
      } else {
        null
      }

  companion object {
    @JvmStatic
    fun wrap(drawableFactory: DrawableFactory): ImageOptionsDrawableFactory {
      return if (drawableFactory is ImageOptionsDrawableFactory) {
        drawableFactory
      } else {
        DrawableFactoryWrapper(drawableFactory)
      }
    }
  }
}

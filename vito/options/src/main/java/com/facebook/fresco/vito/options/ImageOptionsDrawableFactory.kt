/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.options

import android.content.res.Resources
import android.graphics.drawable.Drawable
import com.facebook.imagepipeline.image.CloseableImage

interface ImageOptionsDrawableFactory {
  fun createDrawable(
      resources: Resources,
      closeableImage: CloseableImage,
      imageOptions: ImageOptions
  ): Drawable?
}

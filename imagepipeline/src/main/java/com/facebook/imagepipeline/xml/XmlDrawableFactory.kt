/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.xml

import android.graphics.drawable.Drawable
import com.facebook.imagepipeline.drawable.DrawableFactory
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.image.CloseableXml

internal class XmlDrawableFactory : DrawableFactory {
  override fun supportsImageType(image: CloseableImage): Boolean {
    return image is CloseableXml
  }

  override fun createDrawable(image: CloseableImage): Drawable? {
    return (image as? CloseableXml)?.buildDrawable()
  }
}

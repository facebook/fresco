/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.draweesupport;

import android.graphics.drawable.Drawable;
import com.facebook.fresco.vito.drawable.VitoDrawableFactory;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.imagepipeline.drawable.DrawableFactory;
import com.facebook.imagepipeline.image.CloseableImage;
import javax.annotation.Nullable;

public class DrawableFactoryWrapper implements VitoDrawableFactory {

  private final DrawableFactory mDrawableFactory;

  public DrawableFactoryWrapper(DrawableFactory drawableFactory) {
    mDrawableFactory = drawableFactory;
  }

  @Nullable
  @Override
  public Drawable createDrawable(CloseableImage closeableImage, ImageOptions imageOptions) {
    if (mDrawableFactory.supportsImageType(closeableImage)) {
      return mDrawableFactory.createDrawable(closeableImage);
    }
    return null;
  }
}

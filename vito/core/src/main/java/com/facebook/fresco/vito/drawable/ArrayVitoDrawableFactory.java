/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.drawable;

import android.graphics.drawable.Drawable;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.imagepipeline.image.CloseableImage;
import javax.annotation.Nullable;

public class ArrayVitoDrawableFactory implements VitoDrawableFactory {

  private final VitoDrawableFactory[] mDrawableFactories;

  public ArrayVitoDrawableFactory(VitoDrawableFactory... drawableFactories) {
    mDrawableFactories = drawableFactories;
  }

  @Nullable
  @Override
  public Drawable createDrawable(CloseableImage closeableImage, ImageOptions imageOptions) {
    for (VitoDrawableFactory factory : mDrawableFactories) {
      Drawable drawable = factory.createDrawable(closeableImage, imageOptions);
      if (drawable != null) {
        return drawable;
      }
    }
    return null;
  }
}

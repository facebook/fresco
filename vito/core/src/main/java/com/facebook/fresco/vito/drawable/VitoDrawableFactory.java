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

public interface VitoDrawableFactory {

  @Nullable
  Drawable createDrawable(CloseableImage closeableImage, ImageOptions imageOptions);
}

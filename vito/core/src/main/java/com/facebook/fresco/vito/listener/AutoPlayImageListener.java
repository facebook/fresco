/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.listener;

import android.graphics.drawable.Drawable;
import com.facebook.drawee.backends.pipeline.info.ImageOrigin;
import com.facebook.fresco.animation.drawable.AnimatedDrawable2;
import com.facebook.imagepipeline.image.ImageInfo;
import javax.annotation.Nullable;

/** Autoplays animated images */
public class AutoPlayImageListener implements ImageListener {

  private static @Nullable AutoPlayImageListener INSTANCE;

  private AutoPlayImageListener() {}

  public static AutoPlayImageListener getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new AutoPlayImageListener();
    }
    return INSTANCE;
  }

  @Override
  public void onSubmit(long id, Object callerContext) {}

  @Override
  public void onPlaceholderSet(long id, @Nullable Drawable placeholder) {}

  public void onFinalImageSet(
      long id,
      @ImageOrigin int imageOrigin,
      @Nullable ImageInfo imageInfo,
      @Nullable Drawable drawable) {
    if (drawable instanceof AnimatedDrawable2) {
      ((AnimatedDrawable2) drawable).start();
    }
  }

  @Override
  public void onIntermediateImageSet(long id, @Nullable ImageInfo imageInfo) {}

  @Override
  public void onIntermediateImageFailed(long id, Throwable throwable) {}

  @Override
  public void onFailure(long id, @Nullable Drawable error, Throwable throwable) {}

  @Override
  public void onRelease(long id) {}
}

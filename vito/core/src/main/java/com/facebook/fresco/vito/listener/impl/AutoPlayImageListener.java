/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.listener.impl;

import android.graphics.drawable.Drawable;
import com.facebook.drawee.backends.pipeline.info.ImageOrigin;
import com.facebook.fresco.animation.drawable.AnimatedDrawable2;
import com.facebook.fresco.vito.listener.BaseImageListener;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

/** Autoplays animated images */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class AutoPlayImageListener extends BaseImageListener {

  private static @Nullable AutoPlayImageListener INSTANCE;

  private AutoPlayImageListener() {}

  public static AutoPlayImageListener getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new AutoPlayImageListener();
    }
    return INSTANCE;
  }

  @Override
  public void onSubmit(long id, @Nullable Object callerContext) {}

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
  public void onIntermediateImageFailed(long id, @Nullable Throwable throwable) {}

  @Override
  public void onFailure(long id, @Nullable Drawable error, @Nullable Throwable throwable) {}

  @Override
  public void onRelease(long id) {}
}

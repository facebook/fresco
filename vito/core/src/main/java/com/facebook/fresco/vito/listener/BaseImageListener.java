/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.listener;

import android.graphics.drawable.Drawable;
import com.facebook.drawee.backends.pipeline.info.ImageOrigin;
import com.facebook.fresco.ui.common.DimensionsInfo;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.STRICT)
public class BaseImageListener implements ImageListener {

  @Override
  public void onSubmit(long id, @Nullable Object callerContext) {}

  @Override
  public void onPlaceholderSet(long id, @Nullable Drawable placeholder) {}

  @Override
  public void onFinalImageSet(
      long id,
      @ImageOrigin int imageOrigin,
      @Nullable ImageInfo imageInfo,
      @Nullable Drawable drawable) {}

  @Override
  public void onIntermediateImageSet(long id, @Nullable ImageInfo imageInfo) {}

  @Override
  public void onIntermediateImageFailed(long id, @Nullable Throwable throwable) {}

  @Override
  public void onFailure(long id, @Nullable Drawable error, @Nullable Throwable throwable) {}

  @Override
  public void onRelease(long id) {}

  @Override
  public void onImageDrawn(String id, ImageInfo imageInfo, DimensionsInfo dimensionsInfo) {}
}

/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core;

import android.graphics.drawable.Drawable;
import com.facebook.drawee.backends.pipeline.info.ImageOrigin;
import com.facebook.fresco.ui.common.ControllerListener2;
import com.facebook.fresco.vito.listener.ImageListener;
import com.facebook.imagepipeline.image.ImageInfo;
import javax.annotation.Nullable;

public interface CombinedImageListener extends VitoImageRequestListener {

  void setImageListener(@Nullable ImageListener imageListener);

  void setVitoImageRequestListener(@Nullable VitoImageRequestListener vitoImageRequestListener);

  void setControllerListener2(ControllerListener2<ImageInfo> controllerListener2);

  @Nullable
  ImageListener getImageListener();

  @Override
  void onSubmit(
      long id,
      VitoImageRequest imageRequest,
      @Nullable Object callerContext,
      @Nullable ControllerListener2.Extras extras);

  @Override
  void onPlaceholderSet(long id, VitoImageRequest imageRequest, @Nullable Drawable placeholder);

  @Override
  void onFinalImageSet(
      long id,
      VitoImageRequest imageRequest,
      @ImageOrigin int imageOrigin,
      @Nullable ImageInfo imageInfo,
      @Nullable ControllerListener2.Extras extras,
      @Nullable Drawable drawable);

  @Override
  void onIntermediateImageSet(
      long id, VitoImageRequest imageRequest, @Nullable ImageInfo imageInfo);

  @Override
  void onIntermediateImageFailed(long id, VitoImageRequest imageRequest, Throwable throwable);

  @Override
  void onFailure(
      long id,
      VitoImageRequest imageRequest,
      @Nullable Drawable error,
      @Nullable Throwable throwable,
      @Nullable ControllerListener2.Extras extras);

  @Override
  void onRelease(long id, VitoImageRequest imageRequest, ControllerListener2.Extras extras);
}

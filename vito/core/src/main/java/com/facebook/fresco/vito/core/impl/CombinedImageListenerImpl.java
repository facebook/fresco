/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl;

import android.graphics.drawable.Drawable;
import com.facebook.drawee.backends.pipeline.info.ImageOrigin;
import com.facebook.drawee.backends.pipeline.info.internal.ImagePerfControllerListener2;
import com.facebook.fresco.ui.common.BaseControllerListener2;
import com.facebook.fresco.ui.common.ControllerListener2;
import com.facebook.fresco.vito.core.CombinedImageListener;
import com.facebook.fresco.vito.core.VitoImageRequest;
import com.facebook.fresco.vito.core.VitoImageRequestListener;
import com.facebook.fresco.vito.core.VitoUtils;
import com.facebook.fresco.vito.listener.ImageListener;
import com.facebook.imagepipeline.image.ImageInfo;
import javax.annotation.Nullable;

public class CombinedImageListenerImpl implements CombinedImageListener {

  private @Nullable VitoImageRequestListener mVitoImageRequestListener;
  private @Nullable ImageListener mImageListener;
  private ControllerListener2<ImageInfo> mControllerListener2 =
      BaseControllerListener2.getNoOpListener();
  private @Nullable ImagePerfControllerListener2 mImagePerfControllerListener;

  @Override
  public void setImageListener(@Nullable ImageListener imageListener) {
    mImageListener = imageListener;
  }

  @Override
  public void setVitoImageRequestListener(
      @Nullable VitoImageRequestListener vitoImageRequestListener) {
    mVitoImageRequestListener = vitoImageRequestListener;
  }

  @Override
  @Nullable
  public ImageListener getImageListener() {
    return mImageListener;
  }

  @Override
  public void setControllerListener2(ControllerListener2<ImageInfo> controllerListener2) {
    mControllerListener2 = controllerListener2;
  }

  public void setImagePerfControllerListener(
      @Nullable ImagePerfControllerListener2 imagePerfControllerListener) {
    mImagePerfControllerListener = imagePerfControllerListener;
  }

  @Override
  public void onSubmit(
      long id,
      VitoImageRequest imageRequest,
      @Nullable Object callerContext,
      @Nullable ControllerListener2.Extras extras) {
    if (mVitoImageRequestListener != null) {
      mVitoImageRequestListener.onSubmit(id, imageRequest, callerContext, extras);
    }
    if (mImageListener != null) {
      mImageListener.onSubmit(id, callerContext);
    }
    String stringId = VitoUtils.getStringId(id);
    mControllerListener2.onSubmit(stringId, callerContext, extras);
    if (mImagePerfControllerListener != null) {
      mImagePerfControllerListener.onSubmit(stringId, callerContext, extras);
    }
  }

  @Override
  public void onPlaceholderSet(
      long id, VitoImageRequest imageRequest, @Nullable Drawable placeholder) {
    if (mVitoImageRequestListener != null) {
      mVitoImageRequestListener.onPlaceholderSet(id, imageRequest, placeholder);
    }
    if (mImageListener != null) {
      mImageListener.onPlaceholderSet(id, placeholder);
    }
  }

  @Override
  public void onFinalImageSet(
      long id,
      VitoImageRequest imageRequest,
      @ImageOrigin int imageOrigin,
      @Nullable ImageInfo imageInfo,
      @Nullable ControllerListener2.Extras extras,
      @Nullable Drawable drawable) {
    if (mVitoImageRequestListener != null) {
      mVitoImageRequestListener.onFinalImageSet(
          id, imageRequest, imageOrigin, imageInfo, extras, drawable);
    }
    if (mImageListener != null) {
      mImageListener.onFinalImageSet(id, imageOrigin, imageInfo, drawable);
    }
    String stringId = VitoUtils.getStringId(id);
    mControllerListener2.onFinalImageSet(stringId, imageInfo, extras);
    if (mImagePerfControllerListener != null) {
      mImagePerfControllerListener.onFinalImageSet(stringId, imageInfo, extras);
    }
  }

  @Override
  public void onIntermediateImageSet(
      long id, VitoImageRequest imageRequest, @Nullable ImageInfo imageInfo) {
    if (mVitoImageRequestListener != null) {
      mVitoImageRequestListener.onIntermediateImageSet(id, imageRequest, imageInfo);
    }
    if (mImageListener != null) {
      mImageListener.onIntermediateImageSet(id, imageInfo);
    }
    String stringId = VitoUtils.getStringId(id);
    mControllerListener2.onIntermediateImageSet(stringId, imageInfo);
    if (mImagePerfControllerListener != null) {
      mImagePerfControllerListener.onIntermediateImageSet(stringId, imageInfo);
    }
  }

  @Override
  public void onIntermediateImageFailed(
      long id, VitoImageRequest imageRequest, Throwable throwable) {
    if (mVitoImageRequestListener != null) {
      mVitoImageRequestListener.onIntermediateImageFailed(id, imageRequest, throwable);
    }
    if (mImageListener != null) {
      mImageListener.onIntermediateImageFailed(id, throwable);
    }
    String stringId = VitoUtils.getStringId(id);
    mControllerListener2.onIntermediateImageFailed(stringId);
    if (mImagePerfControllerListener != null) {
      mImagePerfControllerListener.onIntermediateImageFailed(stringId);
    }
  }

  @Override
  public void onFailure(
      long id,
      VitoImageRequest imageRequest,
      @Nullable Drawable error,
      @Nullable Throwable throwable,
      @Nullable ControllerListener2.Extras extras) {
    if (mVitoImageRequestListener != null) {
      mVitoImageRequestListener.onFailure(id, imageRequest, error, throwable, extras);
    }
    if (mImageListener != null) {
      mImageListener.onFailure(id, error, throwable);
    }
    String stringId = VitoUtils.getStringId(id);
    mControllerListener2.onFailure(stringId, throwable, extras);
    if (mImagePerfControllerListener != null) {
      mImagePerfControllerListener.onFailure(stringId, throwable, extras);
    }
  }

  @Override
  public void onRelease(long id, VitoImageRequest imageRequest, ControllerListener2.Extras extras) {
    if (mVitoImageRequestListener != null) {
      mVitoImageRequestListener.onRelease(id, imageRequest, extras);
    }
    if (mImageListener != null) {
      mImageListener.onRelease(id);
    }
    String stringId = VitoUtils.getStringId(id);
    mControllerListener2.onRelease(stringId, extras);
    if (mImagePerfControllerListener != null) {
      mImagePerfControllerListener.onRelease(stringId, extras);
    }
  }

  @Override
  public void onReset() {
    if (mImagePerfControllerListener != null) {
      mImagePerfControllerListener.resetState();
    }
  }
}

/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl;

import android.graphics.drawable.Drawable;
import com.facebook.drawee.backends.pipeline.info.ImageOrigin;
import com.facebook.fresco.vito.core.CombinedImageListener;
import com.facebook.fresco.vito.core.VitoImageRequest;
import com.facebook.fresco.vito.core.VitoImageRequestListener;
import com.facebook.fresco.vito.listener.ImageListener;
import com.facebook.imagepipeline.image.ImageInfo;
import javax.annotation.Nullable;

public class CombinedImageListenerImpl implements CombinedImageListener {

  private @Nullable VitoImageRequestListener mVitoImageRequestListener;
  private @Nullable ImageListener mImageListener;

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
  public void onSubmit(long id, VitoImageRequest imageRequest, @Nullable Object callerContext) {
    if (mVitoImageRequestListener != null) {
      mVitoImageRequestListener.onSubmit(id, imageRequest, callerContext);
    }
    if (mImageListener != null) {
      mImageListener.onSubmit(id, callerContext);
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
      @Nullable Drawable drawable) {
    if (mVitoImageRequestListener != null) {
      mVitoImageRequestListener.onFinalImageSet(id, imageRequest, imageOrigin, imageInfo, drawable);
    }
    if (mImageListener != null) {
      mImageListener.onFinalImageSet(id, imageOrigin, imageInfo, drawable);
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
  }

  @Override
  public void onFailure(
      long id,
      VitoImageRequest imageRequest,
      @Nullable Drawable error,
      @Nullable Throwable throwable) {
    if (mVitoImageRequestListener != null) {
      mVitoImageRequestListener.onFailure(id, imageRequest, error, throwable);
    }
    if (mImageListener != null) {
      mImageListener.onFailure(id, error, throwable);
    }
  }

  @Override
  public void onRelease(long id, VitoImageRequest imageRequest) {
    if (mVitoImageRequestListener != null) {
      mVitoImageRequestListener.onRelease(id, imageRequest);
    }
    if (mImageListener != null) {
      mImageListener.onRelease(id);
    }
  }
}

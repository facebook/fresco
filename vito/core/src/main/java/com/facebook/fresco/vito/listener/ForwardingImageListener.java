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
import javax.annotation.Nullable;

public class ForwardingImageListener implements ImageListener {

  @Nullable
  public static ImageListener create(@Nullable ImageListener a, @Nullable ImageListener b) {
    if (a == null) {
      return b;
    }
    if (b == null) {
      return a;
    }
    return new ForwardingImageListener(a, b);
  }

  @Nullable
  public static ImageListener create(
      @Nullable ImageListener a, @Nullable ImageListener b, @Nullable ImageListener c) {
    if (a == null) {
      return create(b, c);
    }
    if (b == null) {
      return create(a, c);
    }
    if (c == null) {
      return create(a, b);
    }
    return new ForwardingImageListener(a, b, c);
  }

  private final ImageListener[] mListeners;

  public ForwardingImageListener(ImageListener... listeners) {
    mListeners = listeners;
  }

  @Override
  public void onSubmit(long id, Object callerContext) {
    for (int i = 0; i < mListeners.length; i++) {
      if (mListeners[i] != null) {
        mListeners[i].onSubmit(id, callerContext);
      }
    }
  }

  @Override
  public void onPlaceholderSet(long id, @Nullable Drawable placeholder) {
    for (int i = 0; i < mListeners.length; i++) {
      if (mListeners[i] != null) {
        mListeners[i].onPlaceholderSet(id, placeholder);
      }
    }
  }

  @Override
  public void onFinalImageSet(
      long id,
      @ImageOrigin int imageOrigin,
      @Nullable ImageInfo imageInfo,
      @Nullable Drawable drawable) {
    for (int i = 0; i < mListeners.length; i++) {
      if (mListeners[i] != null) {
        mListeners[i].onFinalImageSet(id, imageOrigin, imageInfo, drawable);
      }
    }
  }

  @Override
  public void onIntermediateImageSet(long id, @Nullable ImageInfo imageInfo) {
    for (int i = 0; i < mListeners.length; i++) {
      if (mListeners[i] != null) {
        mListeners[i].onIntermediateImageSet(id, imageInfo);
      }
    }
  }

  @Override
  public void onIntermediateImageFailed(long id, Throwable throwable) {
    for (int i = 0; i < mListeners.length; i++) {
      if (mListeners[i] != null) {
        mListeners[i].onIntermediateImageFailed(id, throwable);
      }
    }
  }

  @Override
  public void onFailure(long id, @Nullable Drawable error, @Nullable Throwable throwable) {
    for (int i = 0; i < mListeners.length; i++) {
      if (mListeners[i] != null) {
        mListeners[i].onFailure(id, error, throwable);
      }
    }
  }

  @Override
  public void onRelease(long id) {
    for (int i = 0; i < mListeners.length; i++) {
      if (mListeners[i] != null) {
        mListeners[i].onRelease(id);
      }
    }
  }

  @Override
  public void onImageDrawn(String id, ImageInfo imageInfo, DimensionsInfo dimensionsInfo) {
    for (int i = 0; i < mListeners.length; i++) {
      if (mListeners[i] != null) {
        mListeners[i].onImageDrawn(id, imageInfo, dimensionsInfo);
      }
    }
  }
}

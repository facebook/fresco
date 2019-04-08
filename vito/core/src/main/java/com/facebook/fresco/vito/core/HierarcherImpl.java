/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core;

import android.content.res.Resources;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import androidx.annotation.Nullable;
import com.facebook.common.references.CloseableReference;
import com.facebook.drawee.drawable.ForwardingDrawable;
import com.facebook.drawee.drawable.ScaleTypeDrawable;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.systrace.FrescoSystrace;

public class HierarcherImpl implements Hierarcher {
  private static final Drawable NOP_DRAWABLE = NopDrawable.INSTANCE;

  @Override
  public Drawable buildPlaceholderDrawable(Resources resources, ImageOptions imageOptions) {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("HierarcherImpl#buildPlaceholderDrawable");
    }
    try {
      @Nullable Drawable placeholderDrawable = imageOptions.getPlaceholderDrawable();
      if (placeholderDrawable == null && imageOptions.getPlaceholderRes() != 0) {
        placeholderDrawable = resources.getDrawable(imageOptions.getPlaceholderRes());
      }
      if (placeholderDrawable == null) {
        return NOP_DRAWABLE;
      }
      if (imageOptions.getPlaceholderScaleType() != null) {
        return new ScaleTypeDrawable(placeholderDrawable, imageOptions.getPlaceholderScaleType());
      }
      return placeholderDrawable;
    } finally {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
  }

  @Override
  public Drawable buildProgressDrawable(Resources resources, ImageOptions imageOptions) {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("HierarcherImpl#buildProgressDrawable");
    }
    try {
      if (imageOptions.getProgressRes() == 0 && imageOptions.getProgressDrawable() == null) {
        return NOP_DRAWABLE;
      }
      Drawable progressDrawable = imageOptions.getProgressDrawable();
      if (progressDrawable == null) {
        progressDrawable = resources.getDrawable(imageOptions.getProgressRes());
      }
      if (imageOptions.getProgressScaleType() != null) {
        return new ScaleTypeDrawable(progressDrawable, imageOptions.getProgressScaleType());
      }
      return progressDrawable;
    } finally {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
  }

  @Override
  @Nullable
  public Drawable buildErrorDrawable(Resources resources, ImageOptions imageOptions) {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("HierarcherImpl#buildErrorDrawable");
    }
    try {
      if (imageOptions.getErrorRes() == 0) {
        return null;
      }
      Drawable drawable = resources.getDrawable(imageOptions.getErrorRes());
      if (imageOptions.getErrorScaleType() != null) {
        return new ScaleTypeDrawable(
            drawable, imageOptions.getErrorScaleType(), imageOptions.getErrorFocusPoint());
      }
      return drawable;
    } finally {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
  }

  @Override
  public ForwardingDrawable buildActualImageWrapper(ImageOptions imageOptions) {
    ScaleTypeDrawable wrapper =
        new ScaleTypeDrawable(
            NOP_DRAWABLE,
            imageOptions.getActualImageScaleType(),
            imageOptions.getActualImageFocusPoint());
    ColorFilter actualImageColorFilter = imageOptions.getActualImageColorFilter();
    if (actualImageColorFilter != null) {
      wrapper.setColorFilter(actualImageColorFilter);
    }
    return wrapper;
  }

  @Override
  @Nullable
  public Drawable buildOverlayDrawable(Resources resources, ImageOptions imageOptions) {
    int resId = imageOptions.getOverlayRes();
    return resId == 0 ? null : resources.getDrawable(imageOptions.getOverlayRes());
  }

  @Nullable
  @Override
  public Drawable setupActualImageDrawable(
      FrescoContext frescoContext,
      FrescoDrawable frescoDrawable,
      Resources resources,
      ImageOptions imageOptions,
      CloseableReference<CloseableImage> closeableImage,
      @Nullable ForwardingDrawable actualImageWrapperDrawable,
      boolean wasImmediate) {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("HierarcherImpl#setupActualImageDrawable");
    }
    try {
      Drawable actualDrawable =
          frescoContext
              .getDrawableFactory(resources)
              .createDrawable(closeableImage.get(), imageOptions);

      if (actualImageWrapperDrawable == null) {
        actualImageWrapperDrawable = buildActualImageWrapper(imageOptions);
      }
      actualImageWrapperDrawable.setCurrent(actualDrawable != null ? actualDrawable : NOP_DRAWABLE);
      frescoDrawable.setImage(actualImageWrapperDrawable, closeableImage);

      if (!frescoDrawable.isDefaultLayerIsOn()) {
        if (wasImmediate || imageOptions.getFadeDurationMs() <= 0) {
          frescoDrawable.showImageImmediately();
        } else {
          frescoDrawable.fadeInImage(imageOptions.getFadeDurationMs());
        }
      } else {
        frescoDrawable.setPlaceholderDrawable(null);
      }
      return actualDrawable;
    } finally {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
  }

  @Override
  public void setupOverlayDrawable(
      FrescoContext frescoContext,
      FrescoDrawable frescoDrawable,
      Resources resources,
      ImageOptions imageOptions,
      @Nullable Drawable overlayDrawable) {
    if (overlayDrawable == null) {
      overlayDrawable = buildOverlayDrawable(resources, imageOptions);
    }
    frescoDrawable.setOverlayDrawable(overlayDrawable);
    frescoDrawable.showOverlayImmediately();
  }
}

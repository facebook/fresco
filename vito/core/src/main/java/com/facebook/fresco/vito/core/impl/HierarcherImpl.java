/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl;

import android.content.res.Resources;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import androidx.annotation.Nullable;
import com.facebook.common.references.CloseableReference;
import com.facebook.drawee.drawable.ForwardingDrawable;
import com.facebook.drawee.drawable.InstrumentedDrawable;
import com.facebook.drawee.drawable.ScaleTypeDrawable;
import com.facebook.fresco.vito.core.BaseFrescoDrawable;
import com.facebook.fresco.vito.core.Hierarcher;
import com.facebook.fresco.vito.core.NopDrawable;
import com.facebook.fresco.vito.drawable.RoundingUtils;
import com.facebook.fresco.vito.drawable.VitoDrawableFactory;
import com.facebook.fresco.vito.options.BorderOptions;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.options.RoundingOptions;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.systrace.FrescoSystrace;

public class HierarcherImpl implements Hierarcher {
  private static final Drawable NOP_DRAWABLE = NopDrawable.INSTANCE;

  private final VitoDrawableFactory mDrawableFactory;
  private final RoundingUtils mRoundingUtils;

  public HierarcherImpl(VitoDrawableFactory drawableFactory) {
    mDrawableFactory = drawableFactory;
    mRoundingUtils = new RoundingUtils();
  }

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
      if (imageOptions.getPlaceholderApplyRoundingOptions()) {
        placeholderDrawable = applyRoundingOptions(resources, placeholderDrawable, imageOptions);
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

  protected Drawable applyRoundingOptions(
      Resources resources, Drawable placeholderDrawable, ImageOptions imageOptions) {
    RoundingOptions roundingOptions = imageOptions.getRoundingOptions();
    BorderOptions borderOptions = imageOptions.getBorderOptions();

    return mRoundingUtils.roundedDrawable(
        resources, placeholderDrawable, borderOptions, roundingOptions);
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
  public void setupActualImageWrapper(
      ScaleTypeDrawable actualImageWrapper, ImageOptions imageOptions) {
    actualImageWrapper.setScaleType(imageOptions.getActualImageScaleType());
    actualImageWrapper.setFocusPoint(imageOptions.getActualImageFocusPoint());
    actualImageWrapper.setColorFilter(imageOptions.getActualImageColorFilter());
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
      BaseFrescoDrawable frescoDrawable,
      Resources resources,
      ImageOptions imageOptions,
      CloseableReference<CloseableImage> closeableImage,
      @Nullable ForwardingDrawable actualImageWrapperDrawable,
      boolean wasImmediate,
      InstrumentedDrawable.Listener instrumentedListener) {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("HierarcherImpl#setupActualImageDrawable");
    }
    try {
      Drawable actualDrawable = mDrawableFactory.createDrawable(closeableImage.get(), imageOptions);

      if (actualImageWrapperDrawable == null) {
        actualImageWrapperDrawable = buildActualImageWrapper(imageOptions);
      }
      actualImageWrapperDrawable.setCurrent(actualDrawable != null ? actualDrawable : NOP_DRAWABLE);

      if (instrumentedListener != null) {
        actualImageWrapperDrawable =
            new InstrumentedDrawable(actualImageWrapperDrawable, instrumentedListener);
      }

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
      BaseFrescoDrawable frescoDrawable,
      Resources resources,
      ImageOptions imageOptions,
      @Nullable Drawable cachedOverlayDrawable) {
    if (cachedOverlayDrawable == null) {
      cachedOverlayDrawable = buildOverlayDrawable(resources, imageOptions);
    }
    frescoDrawable.setOverlayDrawable(cachedOverlayDrawable);
    frescoDrawable.showOverlayImmediately();
  }

  @Override
  public void setupDebugOverlayDrawable(
      BaseFrescoDrawable frescoDrawable,
      @Nullable Drawable overlayDrawable,
      @Nullable Drawable debugOverlayDrawable) {
    if (debugOverlayDrawable == null) {
      return;
    }

    if (overlayDrawable == null) {
      overlayDrawable = debugOverlayDrawable;
    } else {
      overlayDrawable = new LayerDrawable(new Drawable[] {overlayDrawable, debugOverlayDrawable});
    }

    frescoDrawable.setOverlayDrawable(overlayDrawable);
    frescoDrawable.showOverlayImmediately();
  }
}

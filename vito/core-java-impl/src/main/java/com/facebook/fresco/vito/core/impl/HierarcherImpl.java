/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl;

import android.content.res.Resources;
import android.graphics.ColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import androidx.annotation.Nullable;
import com.facebook.common.references.CloseableReference;
import com.facebook.drawee.drawable.ForwardingDrawable;
import com.facebook.drawee.drawable.ScaleTypeDrawable;
import com.facebook.fresco.vito.core.NopDrawable;
import com.facebook.fresco.vito.drawable.RoundingUtils;
import com.facebook.fresco.vito.options.BorderOptions;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.options.ImageOptionsDrawableFactory;
import com.facebook.fresco.vito.options.RoundingOptions;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.systrace.FrescoSystrace;
import com.facebook.infer.annotation.Nullsafe;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class HierarcherImpl implements Hierarcher {
  private static final Drawable NOP_DRAWABLE = NopDrawable.INSTANCE;

  private final ImageOptionsDrawableFactory mDrawableFactory;

  public HierarcherImpl(ImageOptionsDrawableFactory drawableFactory) {
    mDrawableFactory = drawableFactory;
  }

  @Nullable
  @Override
  public Drawable buildActualImageDrawable(
      Resources resources,
      ImageOptions imageOptions,
      CloseableReference<CloseableImage> closeableImage) {
    ImageOptionsDrawableFactory drawableFactory = imageOptions.getCustomDrawableFactory();
    if (drawableFactory == null) {
      drawableFactory = mDrawableFactory;
    }
    return drawableFactory.createDrawable(resources, closeableImage.get(), imageOptions);
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
      } else if (placeholderDrawable == null && imageOptions.getPlaceholderColor() != null) {
        placeholderDrawable = new ColorDrawable(imageOptions.getPlaceholderColor());
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

  @Override
  public Drawable applyRoundingOptions(
      Resources resources, Drawable drawable, ImageOptions imageOptions) {
    RoundingOptions roundingOptions = imageOptions.getRoundingOptions();
    BorderOptions borderOptions = imageOptions.getBorderOptions();

    return RoundingUtils.INSTANCE.roundedDrawable(
        resources, drawable, borderOptions, roundingOptions);
  }

  @Override
  @Nullable
  public Drawable buildProgressDrawable(Resources resources, ImageOptions imageOptions) {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("HierarcherImpl#buildProgressDrawable");
    }
    try {
      if (imageOptions.getProgressRes() == 0 && imageOptions.getProgressDrawable() == null) {
        return null;
      }
      Drawable progressDrawable = imageOptions.getProgressDrawable();
      if (progressDrawable == null) {
        progressDrawable = resources.getDrawable(imageOptions.getProgressRes());
      }
      if (progressDrawable == null) {
        return null;
      }
      progressDrawable.setLevel(0);
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
      Drawable drawable = imageOptions.getErrorDrawable();
      if (drawable == null && imageOptions.getErrorRes() != 0) {
        drawable = resources.getDrawable(imageOptions.getErrorRes());
      } else if (drawable == null && imageOptions.getErrorColor() != null) {
        drawable = new ColorDrawable(imageOptions.getErrorColor());
      }
      if (drawable == null) {
        return null;
      }
      if (imageOptions.getErrorApplyRoundingOptions()) {
        drawable = applyRoundingOptions(resources, drawable, imageOptions);
      }
      if (drawable != null && imageOptions.getErrorScaleType() != null) {
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
  public ForwardingDrawable buildActualImageWrapper(
      ImageOptions imageOptions, @Nullable Object callerContext) {
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
      ScaleTypeDrawable actualImageWrapper,
      ImageOptions imageOptions,
      @Nullable Object callerContext) {
    actualImageWrapper.setScaleType(imageOptions.getActualImageScaleType());
    actualImageWrapper.setFocusPoint(imageOptions.getActualImageFocusPoint());
    actualImageWrapper.setColorFilter(imageOptions.getActualImageColorFilter());
  }

  @Override
  @Nullable
  public Drawable buildOverlayDrawable(Resources resources, ImageOptions imageOptions) {
    @Nullable Drawable overlayDrawable = imageOptions.getOverlayDrawable();
    if (overlayDrawable != null) {
      return overlayDrawable;
    }
    int resId = imageOptions.getOverlayRes();
    return resId == 0 ? null : resources.getDrawable(imageOptions.getOverlayRes());
  }
}

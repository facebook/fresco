/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.options;

import android.graphics.ColorFilter;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import androidx.annotation.DrawableRes;
import com.facebook.common.internal.Objects;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.imagepipeline.common.Priority;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class ImageOptions extends DecodedImageOptions {

  private static ImageOptions sDefaultImageOptions =
      new Builder()
          .placeholderScaleType(ScalingUtils.ScaleType.CENTER_INSIDE)
          .progressScaleType(ScalingUtils.ScaleType.CENTER_INSIDE)
          .errorScaleType(ScalingUtils.ScaleType.CENTER_INSIDE)
          .priority(Priority.HIGH)
          .build();

  public static ImageOptions defaults() {
    return sDefaultImageOptions;
  }

  public static void setDefaults(ImageOptions imageOptions) {
    sDefaultImageOptions = imageOptions;
  }

  public static Builder extend(ImageOptions imageOptions) {
    return new Builder(imageOptions);
  }

  public static Builder create() {
    return extend(defaults());
  }

  // Placeholder
  private final @DrawableRes int mPlaceholderRes;
  private final @Nullable Drawable mPlaceholderDrawable;
  private final @Nullable ScalingUtils.ScaleType mPlaceholderScaleType;
  private final @Nullable PointF mPlaceholderFocusPoint;
  private final boolean mPlaceholderApplyRoundingOptions;

  // Progress bar
  private final @DrawableRes int mProgressRes;
  private final @Nullable Drawable mProgressDrawable;
  private final @Nullable ScalingUtils.ScaleType mProgressScaleType;

  // Error
  private final @DrawableRes int mErrorRes;
  private final @Nullable ScalingUtils.ScaleType mErrorScaleType;
  private final @Nullable PointF mErrorFocusPoint;
  private final @Nullable Drawable mErrorDrawable;

  // Actual image
  private final @Nullable ColorFilter mActualImageColorFilter;

  // Overlay
  private final @DrawableRes int mOverlayRes;
  private final @Nullable Drawable mOverlayDrawable;

  private final boolean mResizeToViewport;

  private final int mFadeDurationMs;

  private final boolean mAutoPlay;

  private final @Nullable ImageOptionsDrawableFactory mCustomDrawableFactory;

  private final int mDelayMs;

  public ImageOptions(Builder builder) {
    super(builder);
    mPlaceholderRes = builder.mPlaceholderRes;
    mPlaceholderDrawable = builder.mPlaceholderDrawable;
    mPlaceholderScaleType = builder.mPlaceholderScaleType;
    mPlaceholderFocusPoint = builder.mPlaceholderFocusPoint;
    mPlaceholderApplyRoundingOptions = builder.mPlaceholderApplyRoundingOptions;

    mErrorRes = builder.mErrorRes;
    mErrorScaleType = builder.mErrorScaleType;
    mErrorFocusPoint = builder.mErrorFocusPoint;
    mErrorDrawable = builder.mErrorDrawable;

    mProgressRes = builder.mProgressRes;
    mProgressDrawable = builder.mProgressDrawable;
    mProgressScaleType = builder.mProgressScaleType;

    mOverlayRes = builder.mOverlayRes;
    mOverlayDrawable = builder.mOverlayDrawable;

    mActualImageColorFilter = builder.mActualImageColorFilter;

    mResizeToViewport = builder.mResizeToViewport;

    mFadeDurationMs = builder.mFadeDurationMs;

    mAutoPlay = builder.mAutoPlay;

    mCustomDrawableFactory = builder.mCustomDrawableFactory;

    mDelayMs = builder.mDelayMs;
  }

  public Builder extend() {
    return ImageOptions.extend(this);
  }

  public @DrawableRes int getPlaceholderRes() {
    return mPlaceholderRes;
  }

  public @Nullable Drawable getPlaceholderDrawable() {
    return mPlaceholderDrawable;
  }

  public @Nullable ScalingUtils.ScaleType getPlaceholderScaleType() {
    return mPlaceholderScaleType;
  }

  public @Nullable PointF getPlaceholderFocusPoint() {
    return mPlaceholderFocusPoint;
  }

  public boolean getPlaceholderApplyRoundingOptions() {
    return mPlaceholderApplyRoundingOptions;
  }

  public @DrawableRes int getErrorRes() {
    return mErrorRes;
  }

  public @Nullable ScalingUtils.ScaleType getErrorScaleType() {
    return mErrorScaleType;
  }

  public @Nullable PointF getErrorFocusPoint() {
    return mErrorFocusPoint;
  }

  public @Nullable Drawable getErrorDrawable() {
    return mErrorDrawable;
  }

  public @DrawableRes int getOverlayRes() {
    return mOverlayRes;
  }

  public @Nullable Drawable getOverlayDrawable() {
    return mOverlayDrawable;
  }

  public @DrawableRes int getProgressRes() {
    return mProgressRes;
  }

  public @Nullable Drawable getProgressDrawable() {
    return mProgressDrawable;
  }

  public @Nullable ScalingUtils.ScaleType getProgressScaleType() {
    return mProgressScaleType;
  }

  public @Nullable ColorFilter getActualImageColorFilter() {
    return mActualImageColorFilter;
  }

  public boolean shouldAutoPlay() {
    return mAutoPlay;
  }

  public boolean shouldResizeToViewport() {
    return mResizeToViewport;
  }

  public int getFadeDurationMs() {
    return mFadeDurationMs;
  }

  public @Nullable ImageOptionsDrawableFactory getCustomDrawableFactory() {
    return mCustomDrawableFactory;
  }

  public int getDelayMs() {
    return mDelayMs;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) return false;
    ImageOptions other = (ImageOptions) obj;
    if (mPlaceholderRes != other.mPlaceholderRes
        || !Objects.equal(mPlaceholderDrawable, other.mPlaceholderDrawable)
        || !Objects.equal(mPlaceholderScaleType, other.mPlaceholderScaleType)
        || !Objects.equal(mPlaceholderFocusPoint, other.mPlaceholderFocusPoint)
        || mPlaceholderApplyRoundingOptions != other.mPlaceholderApplyRoundingOptions
        || mErrorRes != other.mErrorRes
        || !Objects.equal(mErrorScaleType, other.mErrorScaleType)
        || !Objects.equal(mErrorFocusPoint, other.mErrorFocusPoint)
        || mOverlayRes != other.mOverlayRes
        || !Objects.equal(mOverlayDrawable, other.mOverlayDrawable)
        || mProgressRes != other.mProgressRes
        || mProgressDrawable != other.mProgressDrawable
        || mProgressScaleType != other.mProgressScaleType
        || !Objects.equal(mActualImageColorFilter, other.mActualImageColorFilter)
        || mResizeToViewport != other.mResizeToViewport
        || mFadeDurationMs != other.mFadeDurationMs
        || mAutoPlay != other.mAutoPlay
        || !Objects.equal(mCustomDrawableFactory, other.mCustomDrawableFactory)
        || mDelayMs != other.mDelayMs
        || mErrorDrawable != other.mErrorDrawable) {
      return false;
    }
    return equalDecodedOptions(other);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + mPlaceholderRes;
    result = 31 * result + (mPlaceholderDrawable != null ? mPlaceholderDrawable.hashCode() : 0);
    result = 31 * result + (mPlaceholderScaleType != null ? mPlaceholderScaleType.hashCode() : 0);
    result = 31 * result + (mPlaceholderFocusPoint != null ? mPlaceholderFocusPoint.hashCode() : 0);
    result = 31 * result + (mPlaceholderApplyRoundingOptions ? 1 : 0);
    result = 31 * result + mErrorRes;
    result = 31 * result + (mErrorScaleType != null ? mErrorScaleType.hashCode() : 0);
    result = 31 * result + (mErrorFocusPoint != null ? mErrorFocusPoint.hashCode() : 0);
    result = 31 * result + (mErrorDrawable != null ? mErrorDrawable.hashCode() : 0);
    result = 31 * result + mOverlayRes;
    result = 31 * result + (mOverlayDrawable != null ? mOverlayDrawable.hashCode() : 0);
    result = 31 * result + (mProgressDrawable != null ? mProgressDrawable.hashCode() : 0);
    result = 31 * result + (mProgressScaleType != null ? mProgressScaleType.hashCode() : 0);
    result =
        31 * result + (mActualImageColorFilter != null ? mActualImageColorFilter.hashCode() : 0);
    result = 31 * result + (mResizeToViewport ? 1 : 0);
    result = 31 * result + mFadeDurationMs;
    result = 31 * result + (mAutoPlay ? 1 : 0);
    result = 31 * result + mProgressRes;
    result = 31 * result + (mCustomDrawableFactory != null ? mCustomDrawableFactory.hashCode() : 0);
    result = 31 * result + mDelayMs;
    return result;
  }

  @Override
  public String toString() {
    return "ImageOptions{" + toStringHelper() + "}";
  }

  @Override
  protected Objects.ToStringHelper toStringHelper() {
    return super.toStringHelper()
        .add("placeholderRes", mPlaceholderRes)
        .add("placeholderDrawable", mPlaceholderDrawable)
        .add("placeholderScaleType", mPlaceholderScaleType)
        .add("placeholderFocusPoint", mPlaceholderFocusPoint)
        .add("placeholderApplyRoundingOptions", mPlaceholderApplyRoundingOptions)
        .add("progressRes", mProgressRes)
        .add("progressDrawable", mProgressDrawable)
        .add("progressScaleType", mProgressScaleType)
        .add("errorRes", mErrorRes)
        .add("errorScaleType", mErrorScaleType)
        .add("errorFocusPoint", mErrorFocusPoint)
        .add("errorDrawable", mErrorDrawable)
        .add("actualImageColorFilter", mActualImageColorFilter)
        .add("overlayRes", mOverlayRes)
        .add("overlayDrawable", mOverlayDrawable)
        .add("resizeToViewport", mResizeToViewport)
        .add("autoPlay", mAutoPlay)
        .add("fadeDurationMs", mFadeDurationMs)
        .add("customDrawableFactory", mCustomDrawableFactory)
        .add("delayMs", mDelayMs);
  }

  public static final class Builder extends DecodedImageOptions.Builder<Builder> {

    private @DrawableRes int mPlaceholderRes;
    private @Nullable Drawable mPlaceholderDrawable;
    private @Nullable ScalingUtils.ScaleType mPlaceholderScaleType;
    private @Nullable PointF mPlaceholderFocusPoint;
    private boolean mPlaceholderApplyRoundingOptions;

    private @DrawableRes int mProgressRes;
    private @Nullable Drawable mProgressDrawable;
    private @Nullable ScalingUtils.ScaleType mProgressScaleType;

    private @DrawableRes int mErrorRes;
    private @Nullable ScalingUtils.ScaleType mErrorScaleType;
    private @Nullable PointF mErrorFocusPoint;
    private @Nullable Drawable mErrorDrawable;

    private @Nullable ColorFilter mActualImageColorFilter;

    private @DrawableRes int mOverlayRes;
    private @Nullable Drawable mOverlayDrawable;

    private boolean mResizeToViewport;
    private boolean mAutoPlay;

    private int mFadeDurationMs;

    private @Nullable ImageOptionsDrawableFactory mCustomDrawableFactory;

    private int mDelayMs;

    private Builder() {
      super();
    }

    private Builder(ImageOptions defaultOptions) {
      super(defaultOptions);
      mPlaceholderRes = defaultOptions.getPlaceholderRes();
      mPlaceholderDrawable = defaultOptions.getPlaceholderDrawable();
      mPlaceholderScaleType = defaultOptions.getPlaceholderScaleType();
      mPlaceholderFocusPoint = defaultOptions.getPlaceholderFocusPoint();
      mPlaceholderApplyRoundingOptions = defaultOptions.getPlaceholderApplyRoundingOptions();

      mProgressRes = defaultOptions.getProgressRes();
      mProgressDrawable = defaultOptions.getProgressDrawable();
      mProgressScaleType = defaultOptions.getProgressScaleType();

      mErrorRes = defaultOptions.getErrorRes();
      mErrorScaleType = defaultOptions.getErrorScaleType();
      mErrorFocusPoint = defaultOptions.getErrorFocusPoint();
      mErrorDrawable = defaultOptions.getErrorDrawable();

      mActualImageColorFilter = defaultOptions.getActualImageColorFilter();

      mOverlayRes = defaultOptions.getOverlayRes();
      mOverlayDrawable = defaultOptions.getOverlayDrawable();

      mResizeToViewport = defaultOptions.shouldResizeToViewport();

      mFadeDurationMs = defaultOptions.getFadeDurationMs();

      mCustomDrawableFactory = defaultOptions.getCustomDrawableFactory();

      mDelayMs = defaultOptions.getDelayMs();
    }

    public Builder placeholder(@Nullable Drawable placeholder) {
      mPlaceholderDrawable = placeholder;
      mPlaceholderRes = 0;
      return getThis();
    }

    public Builder placeholder(
        @Nullable Drawable placeholder, @Nullable ScalingUtils.ScaleType placeholderScaleType) {
      mPlaceholderDrawable = placeholder;
      mPlaceholderScaleType = placeholderScaleType;
      mPlaceholderRes = 0;
      return getThis();
    }

    public Builder placeholderRes(@DrawableRes int placeholderRes) {
      mPlaceholderRes = placeholderRes;
      mPlaceholderDrawable = null;
      return getThis();
    }

    public Builder placeholderRes(
        @DrawableRes int placeholderRes, @Nullable ScalingUtils.ScaleType placeholderScaleType) {
      mPlaceholderRes = placeholderRes;
      mPlaceholderScaleType = placeholderScaleType;
      mPlaceholderDrawable = null;
      return getThis();
    }

    public Builder placeholderScaleType(@Nullable ScalingUtils.ScaleType placeholderScaleType) {
      mPlaceholderScaleType = placeholderScaleType;
      return getThis();
    }

    public Builder placeholderFocusPoint(@Nullable PointF placeholderFocusPoint) {
      mPlaceholderFocusPoint = placeholderFocusPoint;
      return getThis();
    }

    public Builder placeholderApplyRoundingOptions(boolean placeholderApplyRoundingOptions) {
      mPlaceholderApplyRoundingOptions = placeholderApplyRoundingOptions;
      return getThis();
    }

    public Builder errorRes(@DrawableRes int errorRes) {
      mErrorRes = errorRes;
      return getThis();
    }

    public Builder errorScaleType(@Nullable ScalingUtils.ScaleType errorScaleType) {
      mErrorScaleType = errorScaleType;
      return getThis();
    }

    public Builder errorFocusPoint(@Nullable PointF errorFocusPoint) {
      mErrorFocusPoint = errorFocusPoint;
      return getThis();
    }

    public Builder errorDrawable(@Nullable Drawable errorDrawable) {
      mErrorDrawable = errorDrawable;
      return getThis();
    }

    public Builder progress(@Nullable Drawable progress) {
      mProgressDrawable = progress;
      return getThis();
    }

    public Builder progress(Drawable progress, @Nullable ScalingUtils.ScaleType progressScaleType) {
      mProgressDrawable = progress;
      mProgressScaleType = progressScaleType;
      return getThis();
    }

    public Builder progressRes(@DrawableRes int progressRes) {
      mProgressRes = progressRes;
      return getThis();
    }

    public Builder progressRes(
        @DrawableRes int progressRes, @Nullable ScalingUtils.ScaleType progressScaleType) {
      mProgressRes = progressRes;
      mProgressScaleType = progressScaleType;
      return getThis();
    }

    public Builder progressScaleType(@Nullable ScalingUtils.ScaleType progressScaleType) {
      mProgressScaleType = progressScaleType;
      return getThis();
    }

    public Builder overlayRes(@DrawableRes int overlayRes) {
      mOverlayRes = overlayRes;
      mOverlayDrawable = null;
      return getThis();
    }

    public Builder overlay(@Nullable Drawable overlayDrawable) {
      mOverlayDrawable = overlayDrawable;
      mOverlayRes = 0;
      return getThis();
    }

    public Builder colorFilter(@Nullable ColorFilter colorFilter) {
      mActualImageColorFilter = colorFilter;
      return getThis();
    }

    /**
     * Turns on autoplay for animated images
     *
     * @param autoPlay whether to enable autoplay for animated images
     */
    public Builder autoPlay(final boolean autoPlay) {
      mAutoPlay = autoPlay;
      return getThis();
    }

    /**
     * Will resize bitmap to viewport dimensions. Works only if {@link
     * com.facebook.imagepipeline.common.ResizeOptions} are not set. Works only with Vito for now.
     * Please do not use unless you messaged me for details: @defhlt
     *
     * @param resizeToViewport whether to enable this optimization
     */
    public Builder resizeToViewport(boolean resizeToViewport) {
      mResizeToViewport = resizeToViewport;
      return getThis();
    }

    /**
     * Sets the fade duration.
     *
     * @param fadeInDurationMs
     */
    public Builder fadeDurationMs(int fadeInDurationMs) {
      mFadeDurationMs = fadeInDurationMs;
      return getThis();
    }

    /**
     * Set a custom drawable factory to be used to create the actual image drawable.
     *
     * @param drawableFactory the factory to use
     */
    public Builder customDrawableFactory(@Nullable ImageOptionsDrawableFactory drawableFactory) {
      mCustomDrawableFactory = drawableFactory;
      return getThis();
    }

    /**
     * Set an artificial delay for the final image. Useful for running negative tests on image load
     * time. This will apply on top of any "natural" delay like the image fetch time.
     *
     * @param delayMs The delay to introduce, in milliseconds.
     */
    public Builder delayMs(int delayMs) {
      mDelayMs = delayMs;
      return getThis();
    }

    @Override
    public final ImageOptions build() {
      return new ImageOptions(this);
    }
  }
}

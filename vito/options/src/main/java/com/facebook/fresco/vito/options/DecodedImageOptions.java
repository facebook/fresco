/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.options;

import android.graphics.Bitmap;
import android.graphics.PointF;
import com.facebook.common.internal.Objects;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.common.RotationOptions;
import com.facebook.imagepipeline.request.Postprocessor;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.STRICT)
public class DecodedImageOptions extends EncodedImageOptions {

  public static Builder create() {
    return new Builder(ImageOptions.defaults());
  }

  private final @Nullable ResizeOptions mResizeOptions;
  private final @Nullable RotationOptions mRotationOptions;
  private final @Nullable Postprocessor mPostprocessor;
  private final @Nullable ImageDecodeOptions mImageDecodeOptions;
  private final @Nullable RoundingOptions mRoundingOptions;
  private final @Nullable BorderOptions mBorderOptions;
  private final @Nullable ScalingUtils.ScaleType mActualImageScaleType;
  private final @Nullable PointF mActualImageFocusPoint;
  private final boolean mLocalThumbnailPreviewsEnabled;
  private final @Nullable Bitmap.Config mBitmapConfig;

  public DecodedImageOptions(Builder builder) {
    super(builder);
    mResizeOptions = builder.mResizeOptions;
    mRotationOptions = builder.mRotationOptions;
    mPostprocessor = builder.mPostprocessor;
    mImageDecodeOptions = builder.mImageDecodeOptions;
    mRoundingOptions = builder.mRoundingOptions;
    mBorderOptions = builder.mBorderOptions;
    mActualImageScaleType = builder.mActualImageScaleType;
    mActualImageFocusPoint = builder.mActualFocusPoint;
    mLocalThumbnailPreviewsEnabled = builder.mLocalThumbnailPreviewsEnabled;
    mBitmapConfig = builder.mBitmapConfig;
  }

  public @Nullable ResizeOptions getResizeOptions() {
    return mResizeOptions;
  }

  public @Nullable RotationOptions getRotationOptions() {
    return mRotationOptions;
  }

  public @Nullable Postprocessor getPostprocessor() {
    return mPostprocessor;
  }

  public @Nullable ImageDecodeOptions getImageDecodeOptions() {
    return mImageDecodeOptions;
  }

  public @Nullable RoundingOptions getRoundingOptions() {
    return mRoundingOptions;
  }

  public @Nullable BorderOptions getBorderOptions() {
    return mBorderOptions;
  }

  public @Nullable ScalingUtils.ScaleType getActualImageScaleType() {
    return mActualImageScaleType;
  }

  public @Nullable PointF getActualImageFocusPoint() {
    return mActualImageFocusPoint;
  }

  public boolean areLocalThumbnailPreviewsEnabled() {
    return mLocalThumbnailPreviewsEnabled;
  }

  public @Nullable Bitmap.Config getBitmapConfig() {
    return mBitmapConfig;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) return false;
    DecodedImageOptions other = (DecodedImageOptions) obj;
    return equalDecodedOptions(other);
  }

  protected boolean equalDecodedOptions(DecodedImageOptions other) {
    if (!Objects.equal(mResizeOptions, other.mResizeOptions)
        || !Objects.equal(mRotationOptions, other.mRotationOptions)
        || !Objects.equal(mPostprocessor, other.mPostprocessor)
        || !Objects.equal(mImageDecodeOptions, other.mImageDecodeOptions)
        || !Objects.equal(mRoundingOptions, other.mRoundingOptions)
        || !Objects.equal(mBorderOptions, other.mBorderOptions)
        || !Objects.equal(mActualImageScaleType, other.mActualImageScaleType)
        || !Objects.equal(mActualImageFocusPoint, other.mActualImageFocusPoint)
        || mLocalThumbnailPreviewsEnabled != other.mLocalThumbnailPreviewsEnabled
        || !Objects.equal(mBitmapConfig, other.mBitmapConfig)) {
      return false;
    }
    return equalEncodedOptions(other);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (mResizeOptions != null ? mResizeOptions.hashCode() : 0);
    result = 31 * result + (mRotationOptions != null ? mRotationOptions.hashCode() : 0);
    result = 31 * result + (mPostprocessor != null ? mPostprocessor.hashCode() : 0);
    result = 31 * result + (mImageDecodeOptions != null ? mImageDecodeOptions.hashCode() : 0);
    result = 31 * result + (mRoundingOptions != null ? mRoundingOptions.hashCode() : 0);
    result = 31 * result + (mBorderOptions != null ? mBorderOptions.hashCode() : 0);
    result = 31 * result + (mActualImageScaleType != null ? mActualImageScaleType.hashCode() : 0);
    result = 31 * result + (mActualImageFocusPoint != null ? mActualImageFocusPoint.hashCode() : 0);
    result = 31 * result + (mLocalThumbnailPreviewsEnabled ? 1 : 0);
    result = 31 * result + (mBitmapConfig != null ? mBitmapConfig.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "DecodedImageOptions{" + toStringHelper().toString() + "}";
  }

  @Override
  protected Objects.ToStringHelper toStringHelper() {
    return super.toStringHelper()
        .add("resizeOptions", mResizeOptions)
        .add("rotationOptions", mResizeOptions)
        .add("postprocessor", mPostprocessor)
        .add("imageDecodeOptions", mImageDecodeOptions)
        .add("roundingOptions", mRoundingOptions)
        .add("borderOptions", mBorderOptions)
        .add("actualImageScaleType", mActualImageScaleType)
        .add("actualImageFocusPoint", mActualImageFocusPoint)
        .add("localThumbnailPreviewsEnabled", mLocalThumbnailPreviewsEnabled)
        .add("bitmapConfig", mBitmapConfig);
  }

  public static class Builder<T extends Builder> extends EncodedImageOptions.Builder<T> {

    private @Nullable ResizeOptions mResizeOptions;
    private @Nullable RotationOptions mRotationOptions;
    private @Nullable Postprocessor mPostprocessor;
    private @Nullable ImageDecodeOptions mImageDecodeOptions;
    private @Nullable RoundingOptions mRoundingOptions;
    private @Nullable BorderOptions mBorderOptions;
    private @Nullable ScalingUtils.ScaleType mActualImageScaleType;
    private @Nullable PointF mActualFocusPoint;
    private boolean mLocalThumbnailPreviewsEnabled = false;
    public @Nullable Bitmap.Config mBitmapConfig;

    protected Builder() {
      super();
    }

    protected Builder(ImageOptions defaultOptions) {
      super(defaultOptions);
      mResizeOptions = defaultOptions.getResizeOptions();
      mRotationOptions = defaultOptions.getRotationOptions();
      mPostprocessor = defaultOptions.getPostprocessor();
      mImageDecodeOptions = defaultOptions.getImageDecodeOptions();
      mRoundingOptions = defaultOptions.getRoundingOptions();
      mBorderOptions = defaultOptions.getBorderOptions();
      mActualImageScaleType = defaultOptions.getActualImageScaleType();
      mActualFocusPoint = defaultOptions.getActualImageFocusPoint();
      mLocalThumbnailPreviewsEnabled = defaultOptions.areLocalThumbnailPreviewsEnabled();
      mBitmapConfig = defaultOptions.getBitmapConfig();
    }

    public T resize(@Nullable ResizeOptions resizeOptions) {
      mResizeOptions = resizeOptions;
      return getThis();
    }

    public T rotate(@Nullable RotationOptions rotationOptions) {
      mRotationOptions = rotationOptions;
      return getThis();
    }

    public T postprocess(@Nullable Postprocessor postprocessor) {
      mPostprocessor = postprocessor;
      return getThis();
    }

    public T imageDecodeOptions(@Nullable ImageDecodeOptions imageDecodeOptions) {
      mImageDecodeOptions = imageDecodeOptions;
      return getThis();
    }

    /**
     * Set the rounding options to be used or null if the image should not be rounded.
     *
     * @param roundingOptions the rounding options to use
     * @return the builder
     */
    public T round(@Nullable RoundingOptions roundingOptions) {
      mRoundingOptions = roundingOptions;
      return getThis();
    }

    public T borders(@Nullable BorderOptions borderOptions) {
      mBorderOptions = borderOptions;
      return getThis();
    }

    public T scale(@Nullable ScalingUtils.ScaleType actualImageScaleType) {
      mActualImageScaleType = actualImageScaleType;
      return getThis();
    }

    public T focusPoint(@Nullable PointF focusPoint) {
      mActualFocusPoint = focusPoint;
      return getThis();
    }

    /**
     * Display local thumbnail previews, for example EXIF thumbnails.
     *
     * @param localThumbnailPreviewsEnabled true if thumbnails should be displayed
     * @return the builder
     */
    public T localThumbnailPreviewsEnabled(boolean localThumbnailPreviewsEnabled) {
      mLocalThumbnailPreviewsEnabled = localThumbnailPreviewsEnabled;
      return getThis();
    }

    public T bitmapConfig(@Nullable Bitmap.Config bitmapConfig) {
      mBitmapConfig = bitmapConfig;
      return getThis();
    }

    @Override
    public DecodedImageOptions build() {
      return new DecodedImageOptions(this);
    }
  }
}

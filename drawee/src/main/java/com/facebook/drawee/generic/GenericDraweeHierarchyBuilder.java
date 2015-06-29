/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawee.generic;

import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.List;

import android.content.res.Resources;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;

import com.facebook.common.internal.Preconditions;

import static com.facebook.drawee.drawable.ScalingUtils.ScaleType;

/**
 * Class to construct a {@link GenericDraweeHierarchy}.
 *
 * <p/> This class does not do deep copies of most of the input parameters. There should be one
 * instance of the hierarchy per DraweeView, so that each hierarchy has a unique set of drawables.
 */
public class GenericDraweeHierarchyBuilder {

  public static final int DEFAULT_FADE_DURATION = 300;
  public static final ScaleType DEFAULT_SCALE_TYPE = ScaleType.CENTER_INSIDE;
  public static final ScaleType DEFAULT_ACTUAL_IMAGE_SCALE_TYPE = ScaleType.CENTER_CROP;

  private Resources mResources;

  private int mFadeDuration;

  private Drawable mPlaceholderImage;
  private @Nullable ScaleType mPlaceholderImageScaleType;

  private Drawable mRetryImage;
  private ScaleType mRetryImageScaleType;

  private Drawable mFailureImage;
  private ScaleType mFailureImageScaleType;

  private Drawable mProgressBarImage;
  private ScaleType mProgressBarImageScaleType;

  private ScaleType mActualImageScaleType;
  private Matrix mActualImageMatrix;
  private PointF mActualImageFocusPoint;
  private ColorFilter mActualImageColorFilter;

  private List<Drawable> mBackgrounds;
  private List<Drawable> mOverlays;
  private Drawable mPressedStateOverlay;

  private RoundingParams mRoundingParams;

  public GenericDraweeHierarchyBuilder(Resources resources) {
    mResources = resources;
    init();
  }

  public static GenericDraweeHierarchyBuilder newInstance(Resources resources) {
    return new GenericDraweeHierarchyBuilder(resources);
  }

  /**
   * Initializes this builder to its defaults.
   */
  private void init() {
    mFadeDuration = DEFAULT_FADE_DURATION;

    mPlaceholderImage = null;
    mPlaceholderImageScaleType = null;

    mRetryImage = null;
    mRetryImageScaleType = null;

    mFailureImage = null;
    mFailureImageScaleType = null;

    mProgressBarImage = null;
    mProgressBarImageScaleType = null;

    mActualImageScaleType = DEFAULT_ACTUAL_IMAGE_SCALE_TYPE;
    mActualImageMatrix = null;
    mActualImageFocusPoint = null;

    mBackgrounds = null;
    mOverlays = null;
    mPressedStateOverlay = null;

    mRoundingParams = null;

    mActualImageColorFilter = null;
  }

  /**
   * Resets this builder to its initial values making it reusable.
   * @return modified instance of this builder
   */
  public GenericDraweeHierarchyBuilder reset() {
    init();
    return this;
  }

  /**
   * Gets resources.
   * @return
   */
  public Resources getResources() {
    return mResources;
  }

  /**
   * Sets the duration of the fade animation.
   * If not set, default value of 300ms will be used.
   * @param fadeDuration duration in milliseconds
   * @return modified instance of this builder
   */
  public GenericDraweeHierarchyBuilder setFadeDuration(int fadeDuration) {
    mFadeDuration = fadeDuration;
    return this;
  }

  public int getFadeDuration() {
    return mFadeDuration;
  }

  /**
   * Sets the placeholder image, with default scale type CENTER_INSIDE. If no placeholder is set,
   * a transparent ColorDrawable will be used.
   * @param placeholderDrawable drawable to be used as placeholder image
   * @return modified instance of this builder
   */
  public GenericDraweeHierarchyBuilder setPlaceholderImage(Drawable placeholderDrawable) {
    return setPlaceholderImage(placeholderDrawable, DEFAULT_SCALE_TYPE);
  }

  /**
   * Sets the placeholder image and scale type. If no placeholder is set, a transparent
   * ColorDrawable will be used.
   * @param placeholderDrawable drawable to be used as placeholder image
   * @param placeholderImageScaleType scale type for the placeholder image
   * @return modified instance of this builder
   */
  public GenericDraweeHierarchyBuilder setPlaceholderImage(
      Drawable placeholderDrawable,
      @Nullable ScaleType placeholderImageScaleType) {
    mPlaceholderImage = placeholderDrawable;
    mPlaceholderImageScaleType = placeholderImageScaleType;
    return this;
  }

  public Drawable getPlaceholderImage() {
    return mPlaceholderImage;
  }

  public @Nullable ScaleType getPlaceholderImageScaleType() {
    return mPlaceholderImageScaleType;
  }

  /**
   * Sets the retry image, with default scale type CENTER_INSIDE.
   * @param retryDrawable drawable to be used as retry image
   * @return modified instance of this builder
   */
  public GenericDraweeHierarchyBuilder setRetryImage(Drawable retryDrawable) {
    return setRetryImage(retryDrawable, DEFAULT_SCALE_TYPE);
  }

  /**
   * Sets the retry image and scale type.
   * @param retryDrawable drawable to be used as retry image
   * @param retryImageScaleType scale type for the retry image
   * @return modified instance of this builder
   */
  public GenericDraweeHierarchyBuilder setRetryImage(
      Drawable retryDrawable,
      ScaleType retryImageScaleType) {
    mRetryImage = retryDrawable;
    mRetryImageScaleType = retryImageScaleType;
    return this;
  }

  public Drawable getRetryImage() {
    return mRetryImage;
  }

  public ScaleType getRetryImageScaleType() {
    return mRetryImageScaleType;
  }

  /**
   * Sets the failure image, with default scale type CENTER_INSIDE.
   * @param failureDrawable drawable to be used as failure image
   * @return modified instance of this builder
   */
  public GenericDraweeHierarchyBuilder setFailureImage(Drawable failureDrawable) {
    return setFailureImage(failureDrawable, DEFAULT_SCALE_TYPE);
  }

  /**
   * Sets the failure image, and scale type.
   * @param failureDrawable drawable to be used as failure image
   * @param failureImageScaleType scale type for the failure image
   * @return modified instance of this builder
   */
  public GenericDraweeHierarchyBuilder setFailureImage(
      Drawable failureDrawable,
      ScaleType failureImageScaleType) {
    mFailureImage = failureDrawable;
    mFailureImageScaleType = failureImageScaleType;
    return this;
  }

  public Drawable getFailureImage() {
    return mFailureImage;
  }

  public ScaleType getFailureImageScaleType() {
    return mFailureImageScaleType;
  }

  /**
   * Sets the progressBar image, with default scale type CENTER_INSIDE.
   * @param progressBarImage drawable to be used as progressBar image
   * @return modified instance of this builder
   */
  public GenericDraweeHierarchyBuilder setProgressBarImage(Drawable progressBarImage) {
    return setProgressBarImage(progressBarImage, DEFAULT_SCALE_TYPE);
  }

  /**
   * Sets the progressBar image, and scale type.
   * @param progressBarImage drawable to be used as progressBar image
   * @param progressBarImageScaleType scale type for the progressBar image
   * @return modified instance of this builder
   */
  public GenericDraweeHierarchyBuilder setProgressBarImage(
      Drawable progressBarImage,
      ScaleType progressBarImageScaleType) {
    mProgressBarImage = progressBarImage;
    mProgressBarImageScaleType = progressBarImageScaleType;
    return this;
  }

  public Drawable getProgressBarImage() {
    return mProgressBarImage;
  }

  public ScaleType getProgressBarImageScaleType() {
    return mProgressBarImageScaleType;
  }

  /**
   * Sets the scale type, and removes the transformation matrix, for the actual image. If scale
   * type is not set, and nor is a transformation matrix, then the actual image will be drawn
   * with scale type CENTER_CROP.
   * @param actualImageScaleType scale type for the actual image
   * @return modified instance of this builder
   */
  public GenericDraweeHierarchyBuilder setActualImageScaleType(ScaleType actualImageScaleType) {
    mActualImageScaleType = actualImageScaleType;
    mActualImageMatrix = null;
    return this;
  }

  public ScaleType getActualImageScaleType() {
    return mActualImageScaleType;
  }

  /**
   * Sets the transformation matrix, and removes the scale type, for the actual image. If matrix
   * is not set, then the image will be drawn without a matrix being applied to it.
   * @param actualImageMatrix matrix for the actual image
   * @return modified instance of this builder
   * @deprecated this is likely not something you want
   */
  @Deprecated
  public GenericDraweeHierarchyBuilder setActualImageMatrix(Matrix actualImageMatrix) {
    mActualImageMatrix = actualImageMatrix;
    mActualImageScaleType = null;
    return this;
  }

  public Matrix getActualImageMatrix() {
    return mActualImageMatrix;
  }

  /**
   * Sets the focus point for the actual image.
   * If scale type FOCUS_CROP is used, focus point will attempted to be centered within a view.
   * Each coordinate is a real number in [0,1] range, in the coordinate system where top-left
   * corner of the image corresponds to (0, 0) and the bottom-right corner corresponds to (1, 1).
   * @param focusPoint focus point of the image
   * @return modified instance of this builder
   */
  public GenericDraweeHierarchyBuilder setActualImageFocusPoint(PointF focusPoint) {
    mActualImageFocusPoint = focusPoint;
    return this;
  }

  public PointF getActualImageFocusPoint() {
    return mActualImageFocusPoint;
  }

  /**
   * Sets the color filter.
   *
   * @param colorFilter color filter to be set
   * @return modified instance of this builder
   */
  public GenericDraweeHierarchyBuilder setActualImageColorFilter(ColorFilter colorFilter) {
    mActualImageColorFilter = colorFilter;
    return this;
  }

  public ColorFilter getActualImageColorFilter() {
    return mActualImageColorFilter;
  }

  /**
   * Sets the backgrounds.
   * Backgrounds are drawn in list order before the rest of the hierarchy and overlays. The
   * first background will be drawn at the bottom.
   * @param backgrounds background drawables
   * @return modified instance of this builder
   */
  public GenericDraweeHierarchyBuilder setBackgrounds(List<Drawable> backgrounds) {
    mBackgrounds = backgrounds;
    return this;
  }

  /**
   * Sets a single background.
   * @param background background drawable
   * @return modified instance of this builder
   */
  public GenericDraweeHierarchyBuilder setBackground(Drawable background) {
    mBackgrounds = Arrays.asList(background);
    return this;
  }

  public List<Drawable> getBackgrounds() {
    return mBackgrounds;
  }

  /**
   * Sets the overlays.
   * Overlays are drawn in list order after the backgrounds and the rest of the hierarchy. The
   * last overlay will be drawn at the top.
   * @param overlays overlay drawables
   * @return modified instance of this builder
   */
  public GenericDraweeHierarchyBuilder setOverlays(List<Drawable> overlays) {
    mOverlays = overlays;
    return this;
  }

  /**
   * Sets a single overlay.
   * @param overlay overlay drawable
   * @return modified instance of this builder
   */
  public GenericDraweeHierarchyBuilder setOverlay(Drawable overlay) {
    mOverlays = Arrays.asList(overlay);
    return this;
  }

  public List<Drawable> getOverlays() {
    return mOverlays;
  }

  /**
   * Sets Overlay for pressed state
   * @param drawable for pressed state
   * @return
   */
  public GenericDraweeHierarchyBuilder setPressedStateOverlay(Drawable drawable) {
    StateListDrawable stateListDrawable = new StateListDrawable();
    stateListDrawable.addState(new int[]{android.R.attr.state_pressed}, drawable);
    mPressedStateOverlay = stateListDrawable;
    return this;
  }

  public Drawable getPressedStateOverlay() {
    return mPressedStateOverlay;
  }

  /**
   * Sets rounding params.
   *
   * @param roundingParams rounding params to be set
   * @return modified instance of this builder
   */
  public GenericDraweeHierarchyBuilder setRoundingParams(RoundingParams roundingParams) {
    mRoundingParams = roundingParams;
    return this;
  }

  public RoundingParams getRoundingParams() {
    return mRoundingParams;
  }

  private void validate() {
    if (mOverlays != null) {
      for (Drawable overlay : mOverlays) {
        Preconditions.checkNotNull(overlay);
      }
    }

    if (mBackgrounds != null) {
      for (Drawable background : mBackgrounds) {
        Preconditions.checkNotNull(background);
      }
    }
  }

  public GenericDraweeHierarchy build() {
    validate();
    return new GenericDraweeHierarchy(this);
  }
}

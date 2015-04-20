/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawee.view;

import javax.annotation.Nullable;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import com.facebook.drawee.R;
import com.facebook.drawee.drawable.AutoRotateDrawable;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.generic.RoundingParams;

/**
 * DraweeView that creates GenericDraweeHierarchy based on XML attributes.
 *
 * Fading animation parameters:
 * @attr ref com.facebook.R.styleable#GenericDraweeView_fadeDuration
 * Images & scale types parameters:
 * @attr ref com.facebook.R.styleable#GenericDraweeView_viewAspectRatio
 * @attr ref com.facebook.R.styleable#GenericDraweeView_placeholderImage
 * @attr ref com.facebook.R.styleable#GenericDraweeView_placeholderImageScaleType
 * @attr ref com.facebook.R.styleable#GenericDraweeView_retryImage
 * @attr ref com.facebook.R.styleable#GenericDraweeView_retryImageScaleType
 * @attr ref com.facebook.R.styleable#GenericDraweeView_failureImage
 * @attr ref com.facebook.R.styleable#GenericDraweeView_failureImageScaleType
 * @attr ref com.facebook.R.styleable#GenericDraweeView_progressBarImage
 * @attr ref com.facebook.R.styleable#GenericDraweeView_progressBarImageScaleType
 * @attr ref com.facebook.R.styleable#GenericDraweeView_progressBarAutoRotateInterval
 * @attr ref com.facebook.R.styleable#GenericDraweeView_actualImageScaleType
 * @attr ref com.facebook.R.styleable#GenericDraweeView_backgroundImage
 * @attr ref com.facebook.R.styleable#GenericDraweeView_overlayImage
 * @attr ref com.facebook.R.styleable#GenericDraweeView_pressedStateOverlayImage
 * Rounding parameters:
 * @attr ref com.facebook.R.styleable#GenericDraweeView_roundAsCircle
 * @attr ref com.facebook.R.styleable#GenericDraweeView_roundedCornerRadius
 * @attr ref com.facebook.R.styleable#GenericDraweeView_roundTopLeft
 * @attr ref com.facebook.R.styleable#GenericDraweeView_roundTopRight
 * @attr ref com.facebook.R.styleable#GenericDraweeView_roundBottomRight
 * @attr ref com.facebook.R.styleable#GenericDraweeView_roundBottomLeft
 * @attr ref com.facebook.R.styleable#GenericDraweeView_roundWithOverlayColor
 * @attr ref com.facebook.R.styleable#GenericDraweeView_roundingBorderWidth
 * @attr ref com.facebook.R.styleable#GenericDraweeView_roundingBorderColor
 */
public class GenericDraweeView extends DraweeView<GenericDraweeHierarchy> {

  private float mAspectRatio = 0;
  private final AspectRatioMeasure.Spec mMeasureSpec = new AspectRatioMeasure.Spec();

  public GenericDraweeView(Context context, GenericDraweeHierarchy hierarchy) {
    super(context);
    setHierarchy(hierarchy);
  }

  public GenericDraweeView(Context context) {
    super(context);
    inflateHierarchy(context, null);
  }

  public GenericDraweeView(Context context, AttributeSet attrs) {
    super(context, attrs);
    inflateHierarchy(context, attrs);
  }

  public GenericDraweeView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    inflateHierarchy(context, attrs);
  }

  private void inflateHierarchy(Context context, @Nullable AttributeSet attrs) {
    Resources resources = context.getResources();

    // fading animation defaults
    int fadeDuration = GenericDraweeHierarchyBuilder.DEFAULT_FADE_DURATION;
    // images & scale types defaults
    int placeholderId = 0;
    ScalingUtils.ScaleType placeholderScaleType
        = GenericDraweeHierarchyBuilder.DEFAULT_SCALE_TYPE;
    int retryImageId = 0;
    ScalingUtils.ScaleType retryImageScaleType =
        GenericDraweeHierarchyBuilder.DEFAULT_SCALE_TYPE;
    int failureImageId = 0;
    ScalingUtils.ScaleType failureImageScaleType =
        GenericDraweeHierarchyBuilder.DEFAULT_SCALE_TYPE;
    int progressBarId = 0;
    ScalingUtils.ScaleType progressBarScaleType =
        GenericDraweeHierarchyBuilder.DEFAULT_SCALE_TYPE;
    ScalingUtils.ScaleType actualImageScaleType =
        GenericDraweeHierarchyBuilder.DEFAULT_ACTUAL_IMAGE_SCALE_TYPE;
    int backgroundId = 0;
    int overlayId = 0;
    int pressedStateOverlayId = 0;
    // rounding defaults
    boolean roundAsCircle = false;
    int roundedCornerRadius = 0;
    boolean roundTopLeft = true;
    boolean roundTopRight = true;
    boolean roundBottomRight = true;
    boolean roundBottomLeft = true;
    int roundWithOverlayColor = 0;
    int roundingBorderWidth = 0;
    int roundingBorderColor = 0;
    int progressBarAutoRotateInterval = 0;


    if (attrs != null) {
      TypedArray gdhAttrs = context.obtainStyledAttributes(
          attrs,
          R.styleable.GenericDraweeView);
      try {
        // fade duration
        fadeDuration = gdhAttrs.getInt(
            R.styleable.GenericDraweeView_fadeDuration,
            fadeDuration);

        // aspect ratio
        mAspectRatio = gdhAttrs.getFloat(
            R.styleable.GenericDraweeView_viewAspectRatio,
            mAspectRatio);

        // placeholder image
        placeholderId = gdhAttrs.getResourceId(
            R.styleable.GenericDraweeView_placeholderImage,
            placeholderId);
        // placeholder image scale type
        placeholderScaleType = getScaleTypeFromXml(
            gdhAttrs,
            R.styleable.GenericDraweeView_placeholderImageScaleType,
            placeholderScaleType);

        // retry image
        retryImageId = gdhAttrs.getResourceId(
            R.styleable.GenericDraweeView_retryImage,
            retryImageId);
        // retry image scale type
        retryImageScaleType = getScaleTypeFromXml(
            gdhAttrs,
            R.styleable.GenericDraweeView_retryImageScaleType,
            retryImageScaleType);

        // failure image
        failureImageId = gdhAttrs.getResourceId(
            R.styleable.GenericDraweeView_failureImage,
            failureImageId);
        // failure image scale type
        failureImageScaleType = getScaleTypeFromXml(
            gdhAttrs,
            R.styleable.GenericDraweeView_failureImageScaleType,
            failureImageScaleType);

        // progress bar image
        progressBarId = gdhAttrs.getResourceId(
            R.styleable.GenericDraweeView_progressBarImage,
            progressBarId);
        // progress bar image scale type
        progressBarScaleType = getScaleTypeFromXml(
            gdhAttrs,
            R.styleable.GenericDraweeView_progressBarImageScaleType,
            progressBarScaleType);
        // progress bar auto rotate interval
        progressBarAutoRotateInterval = gdhAttrs.getInteger(
            R.styleable.GenericDraweeView_progressBarAutoRotateInterval,
            0);

        // actual image scale type
        actualImageScaleType = getScaleTypeFromXml(
            gdhAttrs,
            R.styleable.GenericDraweeView_actualImageScaleType,
            actualImageScaleType);

        // background
        backgroundId = gdhAttrs.getResourceId(
            R.styleable.GenericDraweeView_backgroundImage,
            backgroundId);

        // overlay
        overlayId = gdhAttrs.getResourceId(
            R.styleable.GenericDraweeView_overlayImage,
            overlayId);

        // pressedState overlay
        pressedStateOverlayId = gdhAttrs.getResourceId(
            R.styleable.GenericDraweeView_pressedStateOverlayImage,
            pressedStateOverlayId);

        // rounding parameters
        roundAsCircle = gdhAttrs.getBoolean(
            R.styleable.GenericDraweeView_roundAsCircle,
            roundAsCircle);
        roundedCornerRadius = gdhAttrs.getDimensionPixelSize(
            R.styleable.GenericDraweeView_roundedCornerRadius,
            roundedCornerRadius);
        roundTopLeft = gdhAttrs.getBoolean(
            R.styleable.GenericDraweeView_roundTopLeft,
            roundTopLeft);
        roundTopRight = gdhAttrs.getBoolean(
            R.styleable.GenericDraweeView_roundTopRight,
            roundTopRight);
        roundBottomRight = gdhAttrs.getBoolean(
            R.styleable.GenericDraweeView_roundBottomRight,
            roundBottomRight);
        roundBottomLeft = gdhAttrs.getBoolean(
            R.styleable.GenericDraweeView_roundBottomLeft,
            roundBottomLeft);
        roundWithOverlayColor = gdhAttrs.getColor(
            R.styleable.GenericDraweeView_roundWithOverlayColor,
            roundWithOverlayColor);
        roundingBorderWidth = gdhAttrs.getDimensionPixelSize(
            R.styleable.GenericDraweeView_roundingBorderWidth,
            roundingBorderWidth);
        roundingBorderColor = gdhAttrs.getColor(
            R.styleable.GenericDraweeView_roundingBorderColor,
            roundingBorderColor);
      }
      finally {
        gdhAttrs.recycle();
      }
    }

    GenericDraweeHierarchyBuilder builder = new GenericDraweeHierarchyBuilder(resources);
    // set fade duration
    builder.setFadeDuration(fadeDuration);
    // set images & scale types
    if (placeholderId > 0) {
      builder.setPlaceholderImage(resources.getDrawable(placeholderId), placeholderScaleType);
    }
    if (retryImageId > 0) {
      builder.setRetryImage(resources.getDrawable(retryImageId), retryImageScaleType);
    }
    if (failureImageId > 0) {
      builder.setFailureImage(resources.getDrawable(failureImageId), failureImageScaleType);
    }
    if (progressBarId > 0) {
      Drawable progressBarDrawable = resources.getDrawable(progressBarId);
      if (progressBarAutoRotateInterval > 0) {
        progressBarDrawable =
            new AutoRotateDrawable(progressBarDrawable, progressBarAutoRotateInterval);
      }
      builder.setProgressBarImage(progressBarDrawable, progressBarScaleType);
    }
    if (backgroundId > 0) {
      builder.setBackground(resources.getDrawable(backgroundId));
    }
    if (overlayId > 0) {
      builder.setOverlay(resources.getDrawable(overlayId));
    }
    if (pressedStateOverlayId > 0) {
      builder.setPressedStateOverlay(getResources().getDrawable(pressedStateOverlayId));
    }

    builder.setActualImageScaleType(actualImageScaleType);
    // set rounding parameters
    if (roundAsCircle || roundedCornerRadius > 0) {
      RoundingParams roundingParams = new RoundingParams();
      roundingParams.setRoundAsCircle(roundAsCircle);
      if (roundedCornerRadius > 0) {
        roundingParams.setCornersRadii(
            roundTopLeft ? roundedCornerRadius : 0,
            roundTopRight ? roundedCornerRadius : 0,
            roundBottomRight ? roundedCornerRadius : 0,
            roundBottomLeft ? roundedCornerRadius : 0);
      }
      if (roundWithOverlayColor != 0) {
        roundingParams.setOverlayColor(roundWithOverlayColor);
      }
      if (roundingBorderColor != 0 && roundingBorderWidth > 0) {
        roundingParams.setBorder(roundingBorderColor, roundingBorderWidth);
      }
      builder.setRoundingParams(roundingParams);
    }
    setHierarchy(builder.build());
  }

  /**
   * Returns the scale type indicated in XML, or null if the special 'none' value was found.
   */
  private static ScalingUtils.ScaleType getScaleTypeFromXml(
      TypedArray attrs,
      int attrId,
      ScalingUtils.ScaleType defaultScaleType) {
    String xmlType = attrs.getString(attrId);
    return (xmlType != null) ? ScalingUtils.ScaleType.fromString(xmlType) : defaultScaleType;
  }

 /**
  * Sets the desired aspect ratio (w/h).
  */
  public void setAspectRatio(float aspectRatio) {
    if (aspectRatio == mAspectRatio) {
      return;
    }
    mAspectRatio = aspectRatio;
    requestLayout();
  }

  /**
   * Gets the desired aspect ratio (w/h).
   */
  public float getAspectRatio() {
    return mAspectRatio;
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    mMeasureSpec.width = widthMeasureSpec;
    mMeasureSpec.height = heightMeasureSpec;
    AspectRatioMeasure.updateMeasureSpec(
        mMeasureSpec,
        mAspectRatio,
        getLayoutParams(),
        getPaddingLeft() + getPaddingRight(),
        getPaddingTop() + getPaddingBottom());
    super.onMeasure(mMeasureSpec.width, mMeasureSpec.height);
  }
}

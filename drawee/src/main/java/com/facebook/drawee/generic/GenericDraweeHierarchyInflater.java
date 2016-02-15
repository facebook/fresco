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

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import com.facebook.drawee.R;
import com.facebook.drawee.drawable.AutoRotateDrawable;
import static com.facebook.drawee.drawable.ScalingUtils.ScaleType;

/**
 * Inflater for the {@code GenericDraweeHierarchy}.
 *
 * Fading animation parameters:
 * @attr ref com.facebook.R.styleable#GenericDraweeHierarchy_fadeDuration
 * Aspect ratio parameters:
 * @attr ref com.facebook.R.styleable#GenericDraweeHierarchy_viewAspectRatio
 * Images & scale types parameters:
 * @attr ref com.facebook.R.styleable#GenericDraweeHierarchy_placeholderImage
 * @attr ref com.facebook.R.styleable#GenericDraweeHierarchy_placeholderImageScaleType
 * @attr ref com.facebook.R.styleable#GenericDraweeHierarchy_retryImage
 * @attr ref com.facebook.R.styleable#GenericDraweeHierarchy_retryImageScaleType
 * @attr ref com.facebook.R.styleable#GenericDraweeHierarchy_failureImage
 * @attr ref com.facebook.R.styleable#GenericDraweeHierarchy_failureImageScaleType
 * @attr ref com.facebook.R.styleable#GenericDraweeHierarchy_progressBarImage
 * @attr ref com.facebook.R.styleable#GenericDraweeHierarchy_progressBarImageScaleType
 * @attr ref com.facebook.R.styleable#GenericDraweeHierarchy_progressBarAutoRotateInterval
 * @attr ref com.facebook.R.styleable#GenericDraweeHierarchy_actualImageScaleType
 * @attr ref com.facebook.R.styleable#GenericDraweeHierarchy_backgroundImage
 * @attr ref com.facebook.R.styleable#GenericDraweeHierarchy_overlayImage
 * @attr ref com.facebook.R.styleable#GenericDraweeHierarchy_pressedStateOverlayImage
 * Rounding parameters:
 * @attr ref com.facebook.R.styleable#GenericDraweeHierarchy_roundAsCircle
 * @attr ref com.facebook.R.styleable#GenericDraweeHierarchy_roundedCornerRadius
 * @attr ref com.facebook.R.styleable#GenericDraweeHierarchy_roundTopLeft
 * @attr ref com.facebook.R.styleable#GenericDraweeHierarchy_roundTopRight
 * @attr ref com.facebook.R.styleable#GenericDraweeHierarchy_roundBottomRight
 * @attr ref com.facebook.R.styleable#GenericDraweeHierarchy_roundBottomLeft
 * @attr ref com.facebook.R.styleable#GenericDraweeHierarchy_roundWithOverlayColor
 * @attr ref com.facebook.R.styleable#GenericDraweeHierarchy_roundingBorderWidth
 * @attr ref com.facebook.R.styleable#GenericDraweeHierarchy_roundingBorderColor
 * @attr ref com.facebook.R.styleable#GenericDraweeHierarchy_roundingBorderPadding
 */
public class GenericDraweeHierarchyInflater {

  /**
   * Inflates a new hierarchy from XML.
   */
  public static GenericDraweeHierarchy inflateHierarchy(
      Context context,
      @Nullable AttributeSet attrs) {
    return inflateBuilder(context, attrs).build();
  }

  /**
   * Inflates a new hierarchy builder from XML.
   * The builder can then be modified in order to override XML attributes if necessary.
   */
  public static GenericDraweeHierarchyBuilder inflateBuilder(
      Context context,
      @Nullable AttributeSet attrs) {
    Resources resources = context.getResources();
    GenericDraweeHierarchyBuilder builder = new GenericDraweeHierarchyBuilder(resources);
    return updateBuilder(builder, context, attrs);
  }

  /**
   * Updates the existing hierarchy builder based on the XML attributes.
   *
   * This method is useful if a custom view uses different default values. In that case a
   * builder with adjusted default values can be passed to this method and only the properties
   * explicitly specified in XML will be overridden.
   * The builder can be modified afterwards in case some XML attributes needs to be overridden.
   *
   * @param builder a hierarchy builder to be updated
   * @return the modified instance of the same builder
   */
  public static GenericDraweeHierarchyBuilder updateBuilder(
      GenericDraweeHierarchyBuilder builder,
      Context context,
      @Nullable AttributeSet attrs) {
    // these paramters cannot be applied immediately so we store them first
    int progressBarAutoRotateInterval = 0;
    int roundedCornerRadius = 0;
    boolean roundTopLeft = true;
    boolean roundTopRight = true;
    boolean roundBottomLeft = true;
    boolean roundBottomRight = true;

    if (attrs != null) {
      TypedArray gdhAttrs = context.obtainStyledAttributes(
        attrs,
        R.styleable.GenericDraweeHierarchy);
      try {
        final int indexCount = gdhAttrs.getIndexCount();
        for (int i = 0; i < indexCount; i++) {
          final int idx = gdhAttrs.getIndex(i);
          // most popular ones first
          if (idx == R.styleable.GenericDraweeHierarchy_actualImageScaleType) {
            builder.setActualImageScaleType(getScaleTypeFromXml(
              gdhAttrs,
              R.styleable.GenericDraweeHierarchy_actualImageScaleType));

          } else if (idx == R.styleable.GenericDraweeHierarchy_placeholderImage) {
            builder.setPlaceholderImage(getDrawable(
              context,
              gdhAttrs,
              R.styleable.GenericDraweeHierarchy_placeholderImage));

          } else if (idx == R.styleable.GenericDraweeHierarchy_pressedStateOverlayImage) {
            builder.setPressedStateOverlay(getDrawable(
              context,
              gdhAttrs,
              R.styleable.GenericDraweeHierarchy_pressedStateOverlayImage));

          } else if (idx == R.styleable.GenericDraweeHierarchy_progressBarImage) {
            builder.setProgressBarImage(getDrawable(
              context,
              gdhAttrs,
              R.styleable.GenericDraweeHierarchy_progressBarImage));

          // the remaining ones without any particular order
          } else if (idx == R.styleable.GenericDraweeHierarchy_fadeDuration) {
            builder.setFadeDuration(
              gdhAttrs.getInt(R.styleable.GenericDraweeHierarchy_fadeDuration, 0));

          } else if (idx == R.styleable.GenericDraweeHierarchy_viewAspectRatio) {
            builder.setDesiredAspectRatio(
              gdhAttrs.getFloat(R.styleable.GenericDraweeHierarchy_viewAspectRatio, 0));

          } else if (idx == R.styleable.GenericDraweeHierarchy_placeholderImageScaleType) {
            builder.setPlaceholderImageScaleType(getScaleTypeFromXml(
              gdhAttrs,
              R.styleable.GenericDraweeHierarchy_placeholderImageScaleType));

          } else if (idx == R.styleable.GenericDraweeHierarchy_retryImage) {
            builder.setRetryImage(getDrawable(
              context,
              gdhAttrs,
              R.styleable.GenericDraweeHierarchy_retryImage));

          } else if (idx == R.styleable.GenericDraweeHierarchy_retryImageScaleType) {
            builder.setRetryImageScaleType(getScaleTypeFromXml(
              gdhAttrs,
              R.styleable.GenericDraweeHierarchy_retryImageScaleType));

          } else if (idx == R.styleable.GenericDraweeHierarchy_failureImage) {
            builder.setFailureImage(getDrawable(
              context,
              gdhAttrs,
              R.styleable.GenericDraweeHierarchy_failureImage));

          } else if (idx == R.styleable.GenericDraweeHierarchy_failureImageScaleType) {
            builder.setFailureImageScaleType(getScaleTypeFromXml(
              gdhAttrs,
              R.styleable.GenericDraweeHierarchy_failureImageScaleType));

          } else if (idx == R.styleable.GenericDraweeHierarchy_progressBarImageScaleType) {
            builder.setProgressBarImageScaleType(getScaleTypeFromXml(
              gdhAttrs,
              R.styleable.GenericDraweeHierarchy_progressBarImageScaleType));

          } else if (idx == R.styleable.GenericDraweeHierarchy_progressBarAutoRotateInterval) {
            progressBarAutoRotateInterval = gdhAttrs.getInteger(
              R.styleable.GenericDraweeHierarchy_progressBarAutoRotateInterval,
              progressBarAutoRotateInterval);

          } else if (idx == R.styleable.GenericDraweeHierarchy_backgroundImage) {
            builder.setBackground(getDrawable(
              context,
              gdhAttrs,
              R.styleable.GenericDraweeHierarchy_backgroundImage));

          } else if (idx == R.styleable.GenericDraweeHierarchy_overlayImage) {
            builder.setOverlay(getDrawable(
              context,
              gdhAttrs,
              R.styleable.GenericDraweeHierarchy_overlayImage));

          } else if (idx == R.styleable.GenericDraweeHierarchy_roundAsCircle) {
            getRoundingParams(builder).setRoundAsCircle(
              gdhAttrs.getBoolean(R.styleable.GenericDraweeHierarchy_roundAsCircle, false));

          } else if (idx == R.styleable.GenericDraweeHierarchy_roundedCornerRadius) {
            roundedCornerRadius = gdhAttrs.getDimensionPixelSize(
              R.styleable.GenericDraweeHierarchy_roundedCornerRadius,
              roundedCornerRadius);

          } else if (idx == R.styleable.GenericDraweeHierarchy_roundTopLeft) {
            roundTopLeft = gdhAttrs.getBoolean(
              R.styleable.GenericDraweeHierarchy_roundTopLeft,
              roundTopLeft);

          } else if (idx == R.styleable.GenericDraweeHierarchy_roundTopRight) {
            roundTopRight = gdhAttrs.getBoolean(
              R.styleable.GenericDraweeHierarchy_roundTopRight,
              roundTopRight);

          } else if (idx == R.styleable.GenericDraweeHierarchy_roundBottomLeft) {
            roundBottomLeft = gdhAttrs.getBoolean(
              R.styleable.GenericDraweeHierarchy_roundBottomLeft,
              roundBottomLeft);

          } else if (idx == R.styleable.GenericDraweeHierarchy_roundBottomRight) {
            roundBottomRight = gdhAttrs.getBoolean(
              R.styleable.GenericDraweeHierarchy_roundBottomRight,
              roundBottomRight);

          } else if (idx == R.styleable.GenericDraweeHierarchy_roundWithOverlayColor) {
            getRoundingParams(builder).setOverlayColor(gdhAttrs.getColor(
              R.styleable.GenericDraweeHierarchy_roundWithOverlayColor,
              0));

          } else if (idx == R.styleable.GenericDraweeHierarchy_roundingBorderWidth) {
            getRoundingParams(builder).setBorderWidth(gdhAttrs.getDimensionPixelSize(
              R.styleable.GenericDraweeHierarchy_roundingBorderWidth,
              0));

          } else if (idx == R.styleable.GenericDraweeHierarchy_roundingBorderColor) {
            getRoundingParams(builder).setBorderColor(gdhAttrs.getColor(
              R.styleable.GenericDraweeHierarchy_roundingBorderColor,
              0));

          } else if (idx == R.styleable.GenericDraweeHierarchy_roundingBorderPadding) {
            getRoundingParams(builder).setPadding(gdhAttrs.getDimensionPixelSize(
              R.styleable.GenericDraweeHierarchy_roundingBorderPadding,
              0));

          }
        }
      } finally {
        gdhAttrs.recycle();
      }
    }

    // wrap progress bar if auto-rotating requested
    if (builder.getProgressBarImage() != null && progressBarAutoRotateInterval > 0) {
      builder.setProgressBarImage(
        new AutoRotateDrawable(builder.getProgressBarImage(), progressBarAutoRotateInterval));
    }

    // set rounded corner radii if requested
    if (roundedCornerRadius > 0) {
      getRoundingParams(builder).setCornersRadii(
        roundTopLeft ? roundedCornerRadius : 0,
        roundTopRight ? roundedCornerRadius : 0,
        roundBottomRight ? roundedCornerRadius : 0,
        roundBottomLeft ? roundedCornerRadius : 0);
    }
    
    return builder;
  }

  private static RoundingParams getRoundingParams(GenericDraweeHierarchyBuilder builder) {
    if (builder.getRoundingParams() == null) {
      builder.setRoundingParams(new RoundingParams());
    }
    return builder.getRoundingParams();
  }

  @Nullable
  private static Drawable getDrawable(
      Context context,
      TypedArray gdhAttrs,
      int attrId) {
    int resourceId = gdhAttrs.getResourceId(attrId, 0);
    return (resourceId == 0) ? null : context.getResources().getDrawable(resourceId);
  }

  /**
   * Returns the scale type indicated in XML, or null if the special 'none' value was found.
   * Important: these values need to be in sync with GenericDraweeHierarchy styleable attributes.
   */
  @Nullable
  private static ScaleType getScaleTypeFromXml(
      TypedArray gdhAttrs,
      int attrId) {
    switch (gdhAttrs.getInt(attrId, -2)) {
      case -1: // none
        return null;
      case 0: // fitXY
        return ScaleType.FIT_XY;
      case 1: // fitStart
        return ScaleType.FIT_START;
      case 2: // fitCenter
        return ScaleType.FIT_CENTER;
      case 3: // fitEnd
        return ScaleType.FIT_END;
      case 4: // center
        return ScaleType.CENTER;
      case 5: // centerInside
        return ScaleType.CENTER_INSIDE;
      case 6: // centerCrop
        return ScaleType.CENTER_CROP;
      case 7: // focusCrop
        return ScaleType.FOCUS_CROP;
      default:
        // this method is supposed to be called only when XML attribute is specified.
        throw new RuntimeException("XML attribute not specified!");
    }
  }
}

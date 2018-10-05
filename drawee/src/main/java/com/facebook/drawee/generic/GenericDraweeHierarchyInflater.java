/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.generic;

import static com.facebook.drawee.drawable.ScalingUtils.ScaleType;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import com.facebook.drawee.R;
import com.facebook.drawee.drawable.AutoRotateDrawable;
import com.facebook.imagepipeline.systrace.FrescoSystrace;
import com.facebook.infer.annotation.ReturnsOwnership;
import javax.annotation.Nullable;

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
 * @attr ref com.facebook.R.styleable#GenericDraweeHierarchy_roundTopStart
 * @attr ref com.facebook.R.styleable#GenericDraweeHierarchy_roundTopEnd
 * @attr ref com.facebook.R.styleable#GenericDraweeHierarchy_roundBottomStart
 * @attr ref com.facebook.R.styleable#GenericDraweeHierarchy_roundBottomEnd
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
   * Inflates a new hierarchy builder from XML. The builder can then be modified in order to
   * override XML attributes if necessary.
   */
  public static GenericDraweeHierarchyBuilder inflateBuilder(
      Context context, @Nullable AttributeSet attrs) {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("GenericDraweeHierarchyBuilder#inflateBuilder");
    }
    Resources resources = context.getResources();
    GenericDraweeHierarchyBuilder builder = new GenericDraweeHierarchyBuilder(resources);
    builder = updateBuilder(builder, context, attrs);
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
    return builder;
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
    boolean roundTopStart = true;
    boolean roundTopEnd = true;
    boolean roundBottomStart = true;
    boolean roundBottomEnd = true;

    if (attrs != null) {
      TypedArray gdhAttrs = context.obtainStyledAttributes(
        attrs,
        R.styleable.GenericDraweeHierarchy);
      try {
        final int indexCount = gdhAttrs.getIndexCount();
        for (int i = 0; i < indexCount; i++) {
          final int attr = gdhAttrs.getIndex(i);
          // most popular ones first
          if (attr == R.styleable.GenericDraweeHierarchy_actualImageScaleType) {
            builder.setActualImageScaleType(getScaleTypeFromXml(gdhAttrs, attr));

          } else if (attr == R.styleable.GenericDraweeHierarchy_placeholderImage) {
            builder.setPlaceholderImage(getDrawable(context, gdhAttrs, attr));

          } else if (attr == R.styleable.GenericDraweeHierarchy_pressedStateOverlayImage) {
            builder.setPressedStateOverlay(getDrawable(context, gdhAttrs, attr));

          } else if (attr == R.styleable.GenericDraweeHierarchy_progressBarImage) {
            builder.setProgressBarImage(getDrawable(context, gdhAttrs, attr));

          // the remaining ones without any particular order
          } else if (attr == R.styleable.GenericDraweeHierarchy_fadeDuration) {
            builder.setFadeDuration(gdhAttrs.getInt(attr, 0));

          } else if (attr == R.styleable.GenericDraweeHierarchy_viewAspectRatio) {
            builder.setDesiredAspectRatio(gdhAttrs.getFloat(attr, 0));

          } else if (attr == R.styleable.GenericDraweeHierarchy_placeholderImageScaleType) {
            builder.setPlaceholderImageScaleType(getScaleTypeFromXml(gdhAttrs, attr));

          } else if (attr == R.styleable.GenericDraweeHierarchy_retryImage) {
            builder.setRetryImage(getDrawable(context, gdhAttrs, attr));

          } else if (attr == R.styleable.GenericDraweeHierarchy_retryImageScaleType) {
            builder.setRetryImageScaleType(getScaleTypeFromXml(gdhAttrs, attr));

          } else if (attr == R.styleable.GenericDraweeHierarchy_failureImage) {
            builder.setFailureImage(getDrawable(context, gdhAttrs, attr));

          } else if (attr == R.styleable.GenericDraweeHierarchy_failureImageScaleType) {
            builder.setFailureImageScaleType(getScaleTypeFromXml(gdhAttrs, attr));

          } else if (attr == R.styleable.GenericDraweeHierarchy_progressBarImageScaleType) {
            builder.setProgressBarImageScaleType(getScaleTypeFromXml(gdhAttrs, attr));

          } else if (attr == R.styleable.GenericDraweeHierarchy_progressBarAutoRotateInterval) {
            progressBarAutoRotateInterval =
                gdhAttrs.getInteger(attr, progressBarAutoRotateInterval);

          } else if (attr == R.styleable.GenericDraweeHierarchy_backgroundImage) {
            builder.setBackground(getDrawable(context, gdhAttrs, attr));

          } else if (attr == R.styleable.GenericDraweeHierarchy_overlayImage) {
            builder.setOverlay(getDrawable(context, gdhAttrs, attr));

          } else if (attr == R.styleable.GenericDraweeHierarchy_roundAsCircle) {
            getRoundingParams(builder).setRoundAsCircle(gdhAttrs.getBoolean(attr, false));

          } else if (attr == R.styleable.GenericDraweeHierarchy_roundedCornerRadius) {
            roundedCornerRadius = gdhAttrs.getDimensionPixelSize(attr, roundedCornerRadius);

          } else if (attr == R.styleable.GenericDraweeHierarchy_roundTopLeft) {
            roundTopLeft = gdhAttrs.getBoolean(attr, roundTopLeft);

          } else if (attr == R.styleable.GenericDraweeHierarchy_roundTopRight) {
            roundTopRight = gdhAttrs.getBoolean(attr, roundTopRight);

          } else if (attr == R.styleable.GenericDraweeHierarchy_roundBottomLeft) {
            roundBottomLeft = gdhAttrs.getBoolean(attr, roundBottomLeft);

          } else if (attr == R.styleable.GenericDraweeHierarchy_roundBottomRight) {
            roundBottomRight = gdhAttrs.getBoolean(attr, roundBottomRight);

          } else if (attr == R.styleable.GenericDraweeHierarchy_roundTopStart) {
            roundTopStart = gdhAttrs.getBoolean(attr, roundTopStart);

          } else if (attr == R.styleable.GenericDraweeHierarchy_roundTopEnd) {
            roundTopEnd = gdhAttrs.getBoolean(attr, roundTopEnd);

          } else if (attr == R.styleable.GenericDraweeHierarchy_roundBottomStart) {
            roundBottomStart = gdhAttrs.getBoolean(attr, roundBottomStart);

          } else if (attr == R.styleable.GenericDraweeHierarchy_roundBottomEnd) {
            roundBottomEnd = gdhAttrs.getBoolean(attr, roundBottomEnd);

          } else if (attr == R.styleable.GenericDraweeHierarchy_roundWithOverlayColor) {
            getRoundingParams(builder).setOverlayColor(gdhAttrs.getColor(attr, 0));

          } else if (attr == R.styleable.GenericDraweeHierarchy_roundingBorderWidth) {
            getRoundingParams(builder).setBorderWidth(gdhAttrs.getDimensionPixelSize(attr, 0));

          } else if (attr == R.styleable.GenericDraweeHierarchy_roundingBorderColor) {
            getRoundingParams(builder).setBorderColor(gdhAttrs.getColor(attr, 0));

          } else if (attr == R.styleable.GenericDraweeHierarchy_roundingBorderPadding) {
            getRoundingParams(builder).setPadding(gdhAttrs.getDimensionPixelSize(attr, 0));

          }
        }
      } finally {
        gdhAttrs.recycle();

        if (android.os.Build.VERSION.SDK_INT >= 17 && context.getResources().getConfiguration()
            .getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
          roundTopLeft = roundTopLeft && roundTopEnd;
          roundTopRight = roundTopRight && roundTopStart;
          roundBottomRight = roundBottomRight && roundBottomStart;
          roundBottomLeft = roundBottomLeft && roundBottomEnd;
        } else {
          roundTopLeft = roundTopLeft && roundTopStart;
          roundTopRight = roundTopRight && roundTopEnd;
          roundBottomRight = roundBottomRight && roundBottomEnd;
          roundBottomLeft = roundBottomLeft && roundBottomStart;
        }

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

  @ReturnsOwnership
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
      case 8: // fitBottomStart
        return ScaleType.FIT_BOTTOM_START;
      default:
        // this method is supposed to be called only when XML attribute is specified.
        throw new RuntimeException("XML attribute not specified!");
    }
  }
}

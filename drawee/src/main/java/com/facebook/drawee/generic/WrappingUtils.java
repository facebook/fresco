/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.drawee.generic;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Build;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.logging.FLog;
import com.facebook.drawee.drawable.DrawableParent;
import com.facebook.drawee.drawable.ForwardingDrawable;
import com.facebook.drawee.drawable.MatrixDrawable;
import com.facebook.drawee.drawable.Rounded;
import com.facebook.drawee.drawable.RoundedBitmapDrawable;
import com.facebook.drawee.drawable.RoundedColorDrawable;
import com.facebook.drawee.drawable.RoundedCornersDrawable;
import com.facebook.drawee.drawable.RoundedNinePatchDrawable;
import com.facebook.drawee.drawable.ScaleTypeDrawable;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.imagepipeline.systrace.FrescoSystrace;
import javax.annotation.Nullable;

/**
 * A class that contains helper methods for wrapping and rounding.
 */
public class WrappingUtils {

  private static final String TAG = "WrappingUtils";

  // Empty drawable to be temporarily used for hierarchy manipulations.
  //
  // Since drawables are allowed to have at most one parent, and this is a static instance, this
  // drawable may only be used temporarily while carrying some hierarchy manipulations. After those
  // manipulations are done, the drawable must not be owned by any parent anymore.
  //
  // The reason why we need this drawable at all is as follows:
  // Consider Drawable A and its child X. Suppose we want to put X under a new parent B. If we just
  // do B.setCurrent(X), the old parent A still considers X to be its child. If at some later point
  // we do A.setChild(Y), drawable A will clear Drawable.Callback from its old child X, and will set
  // callback to its new child Y. But X is no longer a child of A, and so will A incorrectly remove
  // the callback that B set on X. To avoid that, before setting X as a child of B, we must first
  // remove it from A like so: A.setCurrent(empty); B.setCurrent(X);. In cases where we can't set a
  // null child, we use an empty drawable.
  private static final Drawable sEmptyDrawable = new ColorDrawable(Color.TRANSPARENT);

  /**
   * Wraps the given drawable with a new {@link ScaleTypeDrawable}.
   *
   * <p>If the provided drawable or scale type is null, the given drawable is returned without being
   * wrapped.
   *
   * @return the wrapping scale type drawable, or the original drawable if the wrapping didn't take
   *     place
   */
  @Nullable
  static Drawable maybeWrapWithScaleType(
      @Nullable Drawable drawable, @Nullable ScalingUtils.ScaleType scaleType) {
    return maybeWrapWithScaleType(drawable, scaleType, null);
  }

  /**
   * Wraps the given drawable with a new {@link ScaleTypeDrawable}.
   *
   * <p>If the provided drawable or scale type is null, the given drawable is returned without being
   * wrapped.
   *
   * @return the wrapping scale type drawable, or the original drawable if the wrapping didn't take
   *     place
   */
  @Nullable
  static Drawable maybeWrapWithScaleType(
      @Nullable Drawable drawable,
      @Nullable ScalingUtils.ScaleType scaleType,
      @Nullable PointF focusPoint) {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("WrappingUtils#maybeWrapWithScaleType");
    }
    if (drawable == null || scaleType == null) {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
      return drawable;
    }
    ScaleTypeDrawable scaleTypeDrawable = new ScaleTypeDrawable(drawable, scaleType);
    if (focusPoint != null) {
      scaleTypeDrawable.setFocusPoint(focusPoint);
    }
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
    return scaleTypeDrawable;
  }

  /**
   * Wraps the given drawable with a new {@link MatrixDrawable}.
   *
   * <p> If the provided drawable or matrix is null, the given drawable is returned without
   * being wrapped.
   *
   * @return the wrapping matrix drawable, or the original drawable if the wrapping didn't
   * take place
   */
  @Nullable
  static Drawable maybeWrapWithMatrix(
      @Nullable Drawable drawable,
      @Nullable Matrix matrix) {
    if (drawable == null || matrix == null) {
      return drawable;
    }
    return new MatrixDrawable(drawable, matrix);
  }

  /** Wraps the parent's child with a ScaleTypeDrawable. */
  static ScaleTypeDrawable wrapChildWithScaleType(
      DrawableParent parent, ScalingUtils.ScaleType scaleType) {
    Drawable child = parent.setDrawable(sEmptyDrawable);
    child = maybeWrapWithScaleType(child, scaleType);
    parent.setDrawable(child);
    Preconditions.checkNotNull(child, "Parent has no child drawable!");
    return (ScaleTypeDrawable) child;
  }

  /**
   * Updates the overlay-color rounding of the parent's child drawable.
   *
   * <ul>
   * <li>If rounding mode is OVERLAY_COLOR and the child is not a RoundedCornersDrawable,
   * a new RoundedCornersDrawable is created and the child gets wrapped with it.
   * <li>If rounding mode is OVERLAY_COLOR and the child is already wrapped with a
   * RoundedCornersDrawable, its rounding parameters are updated.
   * <li>If rounding mode is not OVERLAY_COLOR and the child is wrapped with a
   * RoundedCornersDrawable, the rounded drawable gets removed and its child gets
   * attached directly to the parent.
   * </ul>
   */
  static void updateOverlayColorRounding(
      DrawableParent parent,
      @Nullable RoundingParams roundingParams) {
    Drawable child = parent.getDrawable();
    if (roundingParams != null &&
        roundingParams.getRoundingMethod() == RoundingParams.RoundingMethod.OVERLAY_COLOR) {
      // Overlay rounding requested - either update the overlay params or add a new
      // drawable that will do the requested rounding.
      if (child instanceof RoundedCornersDrawable) {
        RoundedCornersDrawable roundedCornersDrawable = (RoundedCornersDrawable) child;
        applyRoundingParams(roundedCornersDrawable, roundingParams);
        roundedCornersDrawable.setOverlayColor(roundingParams.getOverlayColor());
      } else {
        // Important: remove the child before wrapping it with a new parent!
        child = parent.setDrawable(sEmptyDrawable);
        child = maybeWrapWithRoundedOverlayColor(child, roundingParams);
        parent.setDrawable(child);
      }
    } else if (child instanceof RoundedCornersDrawable) {
      // Overlay rounding no longer required so remove drawable that was doing the rounding.
      RoundedCornersDrawable roundedCornersDrawable = (RoundedCornersDrawable) child;
      // Important: remove the child before wrapping it with a new parent!
      child = roundedCornersDrawable.setCurrent(sEmptyDrawable);
      parent.setDrawable(child);
      // roundedCornersDrawable is removed and will get garbage collected, clear the child callback
      sEmptyDrawable.setCallback(null);
    }
  }

  /**
   * Updates the leaf rounding of the parent's child drawable.
   *
   * <ul>
   * <li>If rounding mode is BITMAP_ONLY and the child is not a rounded drawable,
   * it gets rounded with a new rounded drawable.
   * <li>If rounding mode is BITMAP_ONLY and the child is already rounded,
   * its rounding parameters are updated.
   * <li>If rounding mode is not BITMAP_ONLY and the child is rounded,
   * its rounding parameters are reset so that no rounding occurs.
   * </ul>
   */
  static void updateLeafRounding(
      DrawableParent parent,
      @Nullable RoundingParams roundingParams,
      Resources resources) {
    parent = findDrawableParentForLeaf(parent);
    Drawable child = parent.getDrawable();
    if (roundingParams != null &&
        roundingParams.getRoundingMethod() == RoundingParams.RoundingMethod.BITMAP_ONLY) {
      // Leaf rounding requested - either update the params or wrap the current drawable in a
      // drawable that will round it.
      if (child instanceof Rounded) {
        Rounded rounded = (Rounded) child;
        applyRoundingParams(rounded, roundingParams);
      } else if (child != null) {
        // Important: remove the child before wrapping it with a new parent!
        parent.setDrawable(sEmptyDrawable);
        Drawable rounded = applyLeafRounding(child, roundingParams, resources);
        parent.setDrawable(rounded);
      }
    } else if (child instanceof Rounded) {
      // No rounding requested - reset rounding params so no rounding occurs.
      resetRoundingParams((Rounded) child);
    }
  }

  /**
   * Wraps the given drawable with a new {@link RoundedCornersDrawable}.
   *
   * <p> If the provided drawable is null, or if the rounding params do not specify OVERLAY_COLOR
   * mode, the given drawable is returned without being wrapped.
   *
   * @return the wrapping rounded drawable, or the original drawable if the wrapping didn't
   * take place
   */
  static Drawable maybeWrapWithRoundedOverlayColor(
      @Nullable Drawable drawable,
      @Nullable RoundingParams roundingParams) {
    try {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.beginSection("WrappingUtils#maybeWrapWithRoundedOverlayColor");
      }
      if (drawable == null
          || roundingParams == null
          || roundingParams.getRoundingMethod() != RoundingParams.RoundingMethod.OVERLAY_COLOR) {
        return drawable;
      }
      RoundedCornersDrawable roundedCornersDrawable = new RoundedCornersDrawable(drawable);
      applyRoundingParams(roundedCornersDrawable, roundingParams);
      roundedCornersDrawable.setOverlayColor(roundingParams.getOverlayColor());
      return roundedCornersDrawable;
    } finally {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
  }

  /**
   * Applies rounding on the drawable's leaf.
   *
   * <p> Currently only {@link BitmapDrawable} or {@link ColorDrawable} leafs can be rounded.
   * <p> If the leaf cannot be rounded, or the rounding params do not specify BITMAP_ONLY mode,
   * the given drawable is returned without being rounded.
   * <p> If the given drawable is a leaf itself, and it can be rounded, then the rounded drawable
   * is returned.
   * <p> If the given drawable is not a leaf, and its leaf can be rounded, the leaf gets rounded,
   * and the original drawable is returned.
   *
   * @return the rounded drawable, or the original drawable if the rounding didn't take place
   * or it took place on a drawable's child
   */
  static Drawable maybeApplyLeafRounding(
      @Nullable Drawable drawable,
      @Nullable RoundingParams roundingParams,
      Resources resources) {
    try {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.beginSection("WrappingUtils#maybeApplyLeafRounding");
      }
      if (drawable == null
          || roundingParams == null
          || roundingParams.getRoundingMethod() != RoundingParams.RoundingMethod.BITMAP_ONLY) {
        return drawable;
      }
      if (drawable instanceof ForwardingDrawable) {
        DrawableParent parent = findDrawableParentForLeaf((ForwardingDrawable) drawable);
        Drawable child = parent.setDrawable(sEmptyDrawable);
        child = applyLeafRounding(child, roundingParams, resources);
        parent.setDrawable(child);
        return drawable;
      } else {
        return applyLeafRounding(drawable, roundingParams, resources);
      }
    } finally {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
  }

  /**
   * Rounds the given drawable with a {@link RoundedBitmapDrawable} or {@link RoundedColorDrawable}.
   *
   * <p> If the given drawable is not a {@link BitmapDrawable} or a {@link ColorDrawable}, it is
   * returned without being rounded.
   *
   * @return the rounded drawable, or the original drawable if rounding didn't take place
   */
  private static Drawable applyLeafRounding(
      Drawable drawable,
      RoundingParams roundingParams,
      Resources resources) {
    if (drawable instanceof BitmapDrawable) {
      final BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
      RoundedBitmapDrawable roundedBitmapDrawable =
          new RoundedBitmapDrawable(
              resources,
              bitmapDrawable.getBitmap(),
              bitmapDrawable.getPaint());
      applyRoundingParams(roundedBitmapDrawable, roundingParams);
      return roundedBitmapDrawable;
    } else if (drawable instanceof NinePatchDrawable) {
      final NinePatchDrawable ninePatchDrawableDrawable = (NinePatchDrawable) drawable;
      RoundedNinePatchDrawable roundedNinePatchDrawable =
          new RoundedNinePatchDrawable(ninePatchDrawableDrawable);
      applyRoundingParams(roundedNinePatchDrawable, roundingParams);
      return roundedNinePatchDrawable;
    } else if (drawable instanceof ColorDrawable
        && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      RoundedColorDrawable roundedColorDrawable =
          RoundedColorDrawable.fromColorDrawable((ColorDrawable) drawable);
      applyRoundingParams(roundedColorDrawable, roundingParams);
      return roundedColorDrawable;
    } else {
      FLog.w(TAG, "Don't know how to round that drawable: %s", drawable);
    }
    return drawable;
  }

  /**
   * Applies the given rounding params on the specified rounded drawable.
   */
  static void applyRoundingParams(Rounded rounded, RoundingParams roundingParams) {
    rounded.setCircle(roundingParams.getRoundAsCircle());
    rounded.setRadii(roundingParams.getCornersRadii());
    rounded.setBorder(roundingParams.getBorderColor(), roundingParams.getBorderWidth());
    rounded.setPadding(roundingParams.getPadding());
    rounded.setScaleDownInsideBorders(roundingParams.getScaleDownInsideBorders());
    rounded.setPaintFilterBitmap(roundingParams.getPaintFilterBitmap());
  }

  /**
   * Resets the rounding params on the specified rounded drawable, so that no rounding occurs.
   */
  static void resetRoundingParams(Rounded rounded) {
    rounded.setCircle(false);
    rounded.setRadius(0);
    rounded.setBorder(Color.TRANSPARENT, 0);
    rounded.setPadding(0);
    rounded.setScaleDownInsideBorders(false);
    rounded.setPaintFilterBitmap(false);
  }

  /**
   * Finds the immediate parent of a leaf drawable.
   */
  static DrawableParent findDrawableParentForLeaf(DrawableParent parent) {
    while (true) {
      Drawable child = parent.getDrawable();
      if (child == parent || !(child instanceof DrawableParent)) {
        break;
      }
      parent = (DrawableParent) child;
    }
    return parent;
  }
}

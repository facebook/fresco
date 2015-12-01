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

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;

import com.facebook.common.internal.Preconditions;
import com.facebook.drawee.drawable.FadeDrawable;
import com.facebook.drawee.drawable.ForwardingDrawable;
import com.facebook.drawee.drawable.MatrixDrawable;
import com.facebook.drawee.drawable.Rounded;
import com.facebook.drawee.drawable.RoundedBitmapDrawable;
import com.facebook.drawee.drawable.RoundedColorDrawable;
import com.facebook.drawee.drawable.RoundedCornersDrawable;
import com.facebook.drawee.drawable.ScaleTypeDrawable;
import com.facebook.drawee.drawable.SettableDrawable;
import com.facebook.drawee.drawable.VisibilityAwareDrawable;
import com.facebook.drawee.drawable.VisibilityCallback;
import com.facebook.drawee.interfaces.SettableDraweeHierarchy;

import static com.facebook.drawee.drawable.ScalingUtils.ScaleType;

/**
 * A SettableDraweeHierarchy that displays placeholder image until the actual image is set.
 * If provided, failure image will be used in case of failure (placeholder otherwise).
 * If provided, retry image will be used in case of failure when retrying is enabled.
 * If provided, progressbar will be displayed until fully loaded.
 * Each image can be displayed with a different scale type (or no scaling at all).
 * Fading between the layers is supported.
 *
 * <p>
 * Example hierarchy with placeholder, retry, failure and one actual image:
 *  <pre>
 *     o FadeDrawable (top level drawable)
 *     |
 *     +--o ScaleTypeDrawable
 *     |  |
 *     |  +--o Drawable (placeholder image)
 *     |
 *     +--o ScaleTypeDrawable
 *     |  |
 *     |  +--o SettableDrawable
 *     |     |
 *     |     +--o Drawable (actual image)
 *     |
 *     +--o ScaleTypeDrawable
 *     |  |
 *     |  +--o Drawable (retry image)
 *     |
 *     +--o ScaleTypeDrawable
 *        |
 *        +--o Drawable (failure image)
 *  </pre>
 *
 * <p>
 * Note:
 * - ScaleType and Matrix transformations will be added only if specified. If both are unspecified,
 * then the branch for that image will be attached directly.
 * - It is not permitted to set both ScaleType transformation and Matrix transformation for the
 * same image.
 * - A Matrix transformation is only supported for actual image.
 * - All branches (placeholder, failure, retry, actual image, progressBar) are optional.
 * If some branch is not specified it won't be created. The exception is placeholder branch,
 * which will, if not specified, be created with a transparent drawable.
 * - If overlays and/or backgrounds are specified, they are added to the same fade drawable, and
 * are always displayed.
 * - Instance of some drawable should be used by only one DH. If more than one DH is being built
 * with the same builder, different drawable instances must be specified for each DH.
 */
public class GenericDraweeHierarchy implements SettableDraweeHierarchy {

  public static class RootDrawable extends ForwardingDrawable implements VisibilityAwareDrawable {
    @Nullable
    private VisibilityCallback mVisibilityCallback;

    public RootDrawable(Drawable drawable) {
      super(drawable);
    }

    @Override
    public int getIntrinsicWidth() {
      return -1;
    }

    @Override
    public int getIntrinsicHeight() {
      return -1;
    }

    @Override
    public void setVisibilityCallback(@Nullable VisibilityCallback visibilityCallback) {
      mVisibilityCallback = visibilityCallback;
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
      if (mVisibilityCallback != null) {
        mVisibilityCallback.onVisibilityChange(visible);
      }
      return super.setVisible(visible, restart);
    }

    @SuppressLint("WrongCall")
    @Override
    public void draw(Canvas canvas) {
      if (!isVisible()) {
        return;
      }
      if (mVisibilityCallback != null) {
        mVisibilityCallback.onDraw();
      }
      super.draw(canvas);
    }
  }

  private Drawable mEmptyPlaceholderDrawable;
  private final Drawable mEmptyActualImageDrawable = new ColorDrawable(Color.TRANSPARENT);
  private final Drawable mEmptyControllerOverlayDrawable = new ColorDrawable(Color.TRANSPARENT);

  private final Resources mResources;

  private final RootDrawable mTopLevelDrawable;
  private final FadeDrawable mFadeDrawable;
  private final SettableDrawable mActualImageSettableDrawable;

  private final int mPlaceholderImageIndex;
  private final int mProgressBarImageIndex;
  private final int mActualImageIndex;
  private final int mRetryImageIndex;
  private final int mFailureImageIndex;
  private final int mControllerOverlayIndex;

  private RoundingParams mRoundingParams;

  GenericDraweeHierarchy(GenericDraweeHierarchyBuilder builder) {
    mResources = builder.getResources();
    mRoundingParams = builder.getRoundingParams();

    int numLayers = 0;

    // backgrounds
    int numBackgrounds = (builder.getBackgrounds() != null) ? builder.getBackgrounds().size() : 0;
    int backgroundsIndex = numLayers;
    numLayers += numBackgrounds;

    // placeholder image branch
    Drawable placeholderImageBranch = builder.getPlaceholderImage();
    if (placeholderImageBranch == null) {
      placeholderImageBranch = getEmptyPlaceholderDrawable();
    }
    placeholderImageBranch = maybeApplyRoundingBitmapOnly(
        mRoundingParams,
        mResources,
        placeholderImageBranch);
    placeholderImageBranch = maybeWrapWithScaleType(
        placeholderImageBranch,
        builder.getPlaceholderImageScaleType());
    mPlaceholderImageIndex = numLayers++;

    // actual image branch
    Drawable actualImageBranch;
    mActualImageSettableDrawable = new SettableDrawable(mEmptyActualImageDrawable);
    actualImageBranch = mActualImageSettableDrawable;
    actualImageBranch = maybeWrapWithScaleType(
        actualImageBranch,
        builder.getActualImageScaleType(),
        builder.getActualImageFocusPoint());
    actualImageBranch = maybeWrapWithMatrix(
        actualImageBranch,
        builder.getActualImageMatrix());
    actualImageBranch.setColorFilter(builder.getActualImageColorFilter());
    mActualImageIndex = numLayers++;

    // progressBar image branch
    Drawable progressBarImageBranch = builder.getProgressBarImage();
    mProgressBarImageIndex = numLayers++;
    if (progressBarImageBranch != null) {
      progressBarImageBranch = maybeWrapWithScaleType(
          progressBarImageBranch,
          builder.getProgressBarImageScaleType());
    }

    // retry image branch
    Drawable retryImageBranch = builder.getRetryImage();
    mRetryImageIndex = numLayers++;
    if (retryImageBranch != null) {
      retryImageBranch = maybeWrapWithScaleType(
          retryImageBranch,
          builder.getRetryImageScaleType());
    }

    // failure image branch
    Drawable failureImageBranch = builder.getFailureImage();
    mFailureImageIndex = numLayers++;
    if (failureImageBranch != null) {
      failureImageBranch = maybeWrapWithScaleType(
          failureImageBranch,
          builder.getFailureImageScaleType());
    }

    // overlays
    int overlaysIndex = numLayers;
    int numOverlays =
        ((builder.getOverlays() != null) ? builder.getOverlays().size() : 0) +
            ((builder.getPressedStateOverlay() != null) ? 1 : 0);
    numLayers += numOverlays;

    // controller overlay
    mControllerOverlayIndex = numLayers++;

    // array of layers
    Drawable[] layers = new Drawable[numLayers];
    if (numBackgrounds > 0) {
      int index = 0;
      for (Drawable background : builder.getBackgrounds()) {
        layers[backgroundsIndex + index++] =
            maybeApplyRoundingBitmapOnly(mRoundingParams, mResources, background);
      }
    }
    layers[mPlaceholderImageIndex] = placeholderImageBranch;
    layers[mActualImageIndex] = actualImageBranch;
    layers[mProgressBarImageIndex] = progressBarImageBranch;
    layers[mRetryImageIndex] = retryImageBranch;
    layers[mFailureImageIndex] = failureImageBranch;
    if (numOverlays > 0) {
      int index = 0;
      if (builder.getOverlays() != null) {
        for (Drawable overlay : builder.getOverlays()) {
          layers[overlaysIndex + index++] = overlay;
        }
      }
      if (builder.getPressedStateOverlay() != null) {
        layers[overlaysIndex + index++] = builder.getPressedStateOverlay();
      }
    }
    if (mControllerOverlayIndex >= 0) {
      layers[mControllerOverlayIndex] = mEmptyControllerOverlayDrawable;
    }

    // fade drawable composed of branches
    mFadeDrawable = new FadeDrawable(layers);
    mFadeDrawable.setTransitionDuration(builder.getFadeDuration());

    // rounded corners drawable (optional)
    Drawable maybeRoundedDrawable =
        maybeWrapWithRoundedOverlayColor(mRoundingParams, mFadeDrawable);

    // top-level drawable
    mTopLevelDrawable = new RootDrawable(maybeRoundedDrawable);
    mTopLevelDrawable.mutate();

    resetFade();
  }

  private static Drawable maybeWrapWithScaleType(
      Drawable drawable,
      @Nullable ScaleType scaleType) {
    return maybeWrapWithScaleType(drawable, scaleType, null);
  }

  private static Drawable maybeWrapWithScaleType(
      Drawable drawable,
      @Nullable ScaleType scaleType,
      @Nullable PointF focusPoint) {
    Preconditions.checkNotNull(drawable);
    if (scaleType == null) {
      return drawable;
    }
    ScaleTypeDrawable scaleTypeDrawable = new ScaleTypeDrawable(drawable, scaleType);
    if (focusPoint != null) {
      scaleTypeDrawable.setFocusPoint(focusPoint);
    }
    return scaleTypeDrawable;
  }

  private static Drawable maybeWrapWithMatrix(
      Drawable drawable,
      @Nullable Matrix matrix) {
    Preconditions.checkNotNull(drawable);
    if (matrix == null) {
      return drawable;
    }
    return new MatrixDrawable(drawable, matrix);
  }


  private static void applyRoundingParams(Rounded rounded, RoundingParams roundingParams) {
    rounded.setCircle(roundingParams.getRoundAsCircle());
    rounded.setRadii(roundingParams.getCornersRadii());
    rounded.setBorder(
        roundingParams.getBorderColor(),
        roundingParams.getBorderWidth());
  }

  private static Drawable maybeWrapWithRoundedOverlayColor(
      @Nullable RoundingParams roundingParams,
      Drawable drawable) {
    if (roundingParams != null &&
        roundingParams.getRoundingMethod() == RoundingParams.RoundingMethod.OVERLAY_COLOR) {
      RoundedCornersDrawable roundedCornersDrawable = new RoundedCornersDrawable(drawable);
      applyRoundingParams(roundedCornersDrawable, roundingParams);
      roundedCornersDrawable.setOverlayColor(roundingParams.getOverlayColor());
      return roundedCornersDrawable;
    } else {
      return drawable;
    }
  }

  private static Drawable maybeApplyRoundingBitmapOnly(
      @Nullable RoundingParams roundingParams,
      Resources resources,
      Drawable drawable) {
    if (roundingParams == null ||
        roundingParams.getRoundingMethod() != RoundingParams.RoundingMethod.BITMAP_ONLY) {
      return drawable;
    }

    if (drawable instanceof BitmapDrawable || drawable instanceof ColorDrawable) {
      return applyRounding(roundingParams, resources, drawable);
    } else {
      Drawable parent = drawable;
      Drawable child = parent.getCurrent();
      while (child != null && parent != child) {
        if (parent instanceof ForwardingDrawable &&
            (child instanceof BitmapDrawable || child instanceof ColorDrawable)) {
          ((ForwardingDrawable) parent).setCurrent(
              applyRounding(roundingParams, resources, child));
        }
        parent = child;
        child = parent.getCurrent();
      }
    }

    return drawable;
  }

  private static Drawable applyRounding(
      @Nullable RoundingParams roundingParams,
      Resources resources,
      Drawable drawable) {
    if (drawable instanceof BitmapDrawable) {
      RoundedBitmapDrawable roundedBitmapDrawable =
          RoundedBitmapDrawable.fromBitmapDrawable(resources, (BitmapDrawable) drawable);
      applyRoundingParams(roundedBitmapDrawable, roundingParams);
      return roundedBitmapDrawable;
    }
    if (drawable instanceof ColorDrawable &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      RoundedColorDrawable roundedColorDrawable =
          RoundedColorDrawable.fromColorDrawable((ColorDrawable) drawable);
      applyRoundingParams(roundedColorDrawable, roundingParams);
      return roundedColorDrawable;
    }
    return drawable;
  }

  private void resetActualImages() {
    if (mActualImageSettableDrawable != null) {
      mActualImageSettableDrawable.setDrawable(mEmptyActualImageDrawable);
    }
  }

  private void resetFade() {
    if (mFadeDrawable != null) {
      mFadeDrawable.beginBatchMode();
      // turn on all layers (backgrounds, branches, overlays)
      mFadeDrawable.fadeInAllLayers();
      // turn off branches (leaving backgrounds and overlays on)
      fadeOutBranches();
      // turn on placeholder
      fadeInLayer(mPlaceholderImageIndex);
      mFadeDrawable.finishTransitionImmediately();
      mFadeDrawable.endBatchMode();
    }
  }

  private void fadeOutBranches() {
    fadeOutLayer(mPlaceholderImageIndex);
    fadeOutLayer(mActualImageIndex);
    fadeOutLayer(mProgressBarImageIndex);
    fadeOutLayer(mRetryImageIndex);
    fadeOutLayer(mFailureImageIndex);
  }

  private void fadeInLayer(int index) {
    if (index >= 0) {
      mFadeDrawable.fadeInLayer(index);
    }
  }

  private void fadeOutLayer(int index) {
    if (index >= 0) {
      mFadeDrawable.fadeOutLayer(index);
    }
  }

  private void setProgress(float progress) {
    Drawable progressBarDrawable = getLayerChildDrawable(mProgressBarImageIndex);
    if (progressBarDrawable == null) {
      return;
    }

    // display progressbar when not fully loaded, hide otherwise
    if (progress >= 0.999f) {
      if (progressBarDrawable instanceof Animatable) {
        ((Animatable) progressBarDrawable).stop();
      }
      fadeOutLayer(mProgressBarImageIndex);
    } else {
      if (progressBarDrawable instanceof Animatable) {
        ((Animatable) progressBarDrawable).start();
      }
      fadeInLayer(mProgressBarImageIndex);
    }
    // set drawable level, scaled to [0, 10000] per drawable specification
    progressBarDrawable.setLevel(Math.round(progress * 10000));
  }

  // SettableDraweeHierarchy interface

  @Override
  public Drawable getTopLevelDrawable() {
    return mTopLevelDrawable;
  }

  @Override
  public void reset() {
    resetActualImages();
    resetFade();
  }

  @Override
  public void setImage(Drawable drawable, float progress, boolean immediate) {
    drawable = maybeApplyRoundingBitmapOnly(mRoundingParams, mResources, drawable);
    drawable.mutate();
    mActualImageSettableDrawable.setDrawable(drawable);
    mFadeDrawable.beginBatchMode();
    fadeOutBranches();
    fadeInLayer(mActualImageIndex);
    setProgress(progress);
    if (immediate) {
      mFadeDrawable.finishTransitionImmediately();
    }
    mFadeDrawable.endBatchMode();
  }

  @Override
  public void setProgress(float progress, boolean immediate) {
    mFadeDrawable.beginBatchMode();
    setProgress(progress);
    if (immediate) {
      mFadeDrawable.finishTransitionImmediately();
    }
    mFadeDrawable.endBatchMode();
  }

  @Override
  public void setFailure(Throwable throwable) {
    mFadeDrawable.beginBatchMode();
    fadeOutBranches();
    if (mFadeDrawable.getDrawable(mFailureImageIndex) != null) {
      fadeInLayer(mFailureImageIndex);
    } else {
      fadeInLayer(mPlaceholderImageIndex);
    }
    mFadeDrawable.endBatchMode();
  }

  @Override
  public void setRetry(Throwable throwable) {
    mFadeDrawable.beginBatchMode();
    fadeOutBranches();
    if (mFadeDrawable.getDrawable(mRetryImageIndex) != null) {
      fadeInLayer(mRetryImageIndex);
    } else {
      fadeInLayer(mPlaceholderImageIndex);
    }
    mFadeDrawable.endBatchMode();
  }

  @Override
  public void setControllerOverlay(@Nullable Drawable drawable) {
    if (drawable == null) {
      drawable = mEmptyControllerOverlayDrawable;
    }
    mFadeDrawable.setDrawable(mControllerOverlayIndex, drawable);
  }

  public void setFadeDuration(int durationMs) {
    mFadeDrawable.setTransitionDuration(durationMs);
  }

  // Helper methods for accessing layers

  /**
   * Gets the drawable at the specified index while skipping MatrixDrawable and ScaleTypeDrawable.
   *
   * <p> If <code>returnParent</code> is set, parent drawable will be returned instead. If
   * MatrixDrawable or ScaleTypeDrawable is found at that index, it will be returned as a parent.
   * Otherwise, the FadeDrawable will be returned as a parent.
   */
  private Drawable getLayerDrawable(int index, boolean returnParent) {
    Drawable parent = mFadeDrawable;
    Drawable child = mFadeDrawable.getDrawable(index);
    if (child instanceof MatrixDrawable) {
      parent = child;
      child = parent.getCurrent();
    }
    if (child instanceof ScaleTypeDrawable) {
      parent = child;
      child = parent.getCurrent();
    }
    return returnParent ? parent : child;
  }

  /**
   * Returns the ScaleTypeDrawable at the specified index, or null if not found.
   */
  private @Nullable ScaleTypeDrawable findLayerScaleTypeDrawable(int index) {
    Drawable drawable = mFadeDrawable.getDrawable(index);
    if (drawable instanceof MatrixDrawable) {
      drawable = drawable.getCurrent();
    }
    if (drawable instanceof ScaleTypeDrawable) {
      return (ScaleTypeDrawable) drawable;
    } else {
      return null;
    }
  }

  /**
   * Sets a child drawable at the specified index.
   *
   * <p> Note: This uses {@link #getLayerDrawable} to find the parent drawable. Given drawable is
   * then set as its child.
   */
  private void setLayerChildDrawable(int index, Drawable drawable) {
    Drawable parent = getLayerDrawable(index, true /* returnParent */);
    if (parent == mFadeDrawable) {
      mFadeDrawable.setDrawable(index, drawable);
    } else {
      ((ForwardingDrawable) parent).setCurrent(drawable);
    }
  }

  /**
   * Gets the child drawable at the specified index.
   */
  private Drawable getLayerChildDrawable(int index) {
    return getLayerDrawable(index, false /* returnParent */);
  }

  private Drawable getEmptyPlaceholderDrawable() {
    if (mEmptyPlaceholderDrawable == null) {
      mEmptyPlaceholderDrawable = new ColorDrawable(Color.TRANSPARENT);
    }
    return mEmptyPlaceholderDrawable;
  }

  // Mutability

  /** Sets the actual image focus point. */
  public void setActualImageFocusPoint(PointF focusPoint) {
    Preconditions.checkNotNull(focusPoint);
    ScaleTypeDrawable scaleTypeDrawable = findLayerScaleTypeDrawable(mActualImageIndex);
    if (scaleTypeDrawable == null) {
      throw new UnsupportedOperationException("ScaleTypeDrawable not found!");
    }
    scaleTypeDrawable.setFocusPoint(focusPoint);
  }

  /** Sets the actual image scale type. */
  public void setActualImageScaleType(ScaleType scaleType) {
    Preconditions.checkNotNull(scaleType);
    ScaleTypeDrawable scaleTypeDrawable = findLayerScaleTypeDrawable(mActualImageIndex);
    if (scaleTypeDrawable == null) {
      throw new UnsupportedOperationException("ScaleTypeDrawable not found!");
    }
    scaleTypeDrawable.setScaleType(scaleType);
  }

  /** Sets the color filter to be applied on the actual image. */
  public void setActualImageColorFilter(ColorFilter colorfilter) {
    mFadeDrawable.getDrawable(mActualImageIndex).setColorFilter(colorfilter);
  }

  /**
   * Gets the post-scaling bounds of the actual image.
   *
   * <p> Note: the returned bounds are not cropped.
   * @param outBounds rect to fill with bounds
   */
  public void getActualImageBounds(RectF outBounds) {
    mActualImageSettableDrawable.getTransformedBounds(outBounds);
  }

  /**
   * Sets a new placeholder drawable.
   *
   * <p>The placeholder scale type will not be changed.
   */
  public void setPlaceholderImage(Drawable drawable) {
    setPlaceholderImage(drawable, null);
  }

  public void setPlaceholderImage(@Nullable Drawable drawable, @Nullable ScaleType scaleType) {
    if (drawable == null) {
      drawable = getEmptyPlaceholderDrawable();
    }

    setDrawableAndScaleType(drawable, scaleType, mPlaceholderImageIndex);
  }

  public void setPlaceholderImageFocusPoint(PointF focusPoint) {
    Preconditions.checkNotNull(focusPoint);
    ScaleTypeDrawable scaleTypeDrawable = findLayerScaleTypeDrawable(mPlaceholderImageIndex);
    if (scaleTypeDrawable == null) {
      throw new UnsupportedOperationException("ScaleTypeDrawable not found!");
    }
    scaleTypeDrawable.setFocusPoint(focusPoint);
  }

  /**
   * Sets a new placeholder drawable using the supplied resource ID.
   *
   * <p>The placeholder scale type will not be changed.
   * @param resourceId an identifier of an Android drawable or color resource.
   */
  public void setPlaceholderImage(int resourceId) {
    setPlaceholderImage(mResources.getDrawable(resourceId));
  }

  /**
   * Sets a new failure drawable.
   */
  public void setFailureImage(Drawable drawable) {
    setFailureImage(drawable, null);
  }

  /**
   * Sets a new failure drawable and scale type.
   */
  public void setFailureImage(@Nullable Drawable drawable, @Nullable ScaleType scaleType) {
    setDrawableAndScaleType(drawable, scaleType, mFailureImageIndex);
  }

  /**
   * Sets a new retry drawable.
   */
  public void setRetryImage(Drawable drawable) {
    setRetryImage(drawable, null);
  }

  /**
   * Sets a new retry drawable and scale type.
   */
  public void setRetryImage(@Nullable Drawable drawable, @Nullable ScaleType scaleType) {
    setDrawableAndScaleType(drawable, scaleType, mRetryImageIndex);
  }

  /**
   * Sets a new progress bar drawable.
   */
  public void setProgressBarImage(Drawable drawable) {
    setProgressBarImage(drawable, null);
  }

  /**
   * Sets a new progress bar drawable and scale type.
   */
  public void setProgressBarImage(@Nullable Drawable drawable, @Nullable ScaleType scaleType) {
    setDrawableAndScaleType(drawable, scaleType, mProgressBarImageIndex);
  }

  private void setDrawableAndScaleType(
      @Nullable Drawable drawable,
      @Nullable ScaleType scaleType,
      int index) {

    if (drawable == null) {
      mFadeDrawable.setDrawable(index, null);
      return;
    }

    drawable = maybeApplyRoundingBitmapOnly(mRoundingParams, mResources, drawable);
    if (scaleType != null) {
      ScaleTypeDrawable scaleTypeDrawable = findLayerScaleTypeDrawable(index);
      if (scaleTypeDrawable != null) {
        scaleTypeDrawable.setScaleType(scaleType);
      } else {
        drawable = maybeWrapWithScaleType(drawable, scaleType);
      }
    }

    setLayerChildDrawable(index, drawable);
  }

  /**
   * Sets the rounding params.
   */
  public void setRoundingParams(RoundingParams roundingParams) {
    mRoundingParams = roundingParams;
    updateOverlayColorRounding();
    updateBitmapOnlyRounding();
  }

  private void updateOverlayColorRounding() {
    Drawable topDrawableChild = mTopLevelDrawable.getCurrent();
    if (mRoundingParams != null &&
        mRoundingParams.getRoundingMethod() == RoundingParams.RoundingMethod.OVERLAY_COLOR) {
      // Overlay rounding requested - either update the overlay params or add a new
      // drawable that will do the requested rounding.
      if (topDrawableChild instanceof RoundedCornersDrawable) {
        RoundedCornersDrawable roundedCornersDrawable = (RoundedCornersDrawable) topDrawableChild;
        applyRoundingParams(roundedCornersDrawable, mRoundingParams);
        roundedCornersDrawable.setOverlayColor(mRoundingParams.getOverlayColor());
      } else {
        // important: remove the child before wrapping it with a new parent!
        topDrawableChild = mTopLevelDrawable.setCurrent(mEmptyActualImageDrawable);
        topDrawableChild = maybeWrapWithRoundedOverlayColor(mRoundingParams, topDrawableChild);
        mTopLevelDrawable.setCurrent(topDrawableChild);
      }
    } else if (topDrawableChild instanceof RoundedCornersDrawable) {
      // Overlay rounding no longer required so remove drawable that was doing the rounding.
      RoundedCornersDrawable roundedCornersDrawable = (RoundedCornersDrawable) topDrawableChild;
      // Extract the drawable out of roundedCornersDrawable before setting it to a new parent.
      topDrawableChild = roundedCornersDrawable.setCurrent(mEmptyActualImageDrawable);
      mTopLevelDrawable.setCurrent(topDrawableChild);
    }
  }

  private void updateBitmapOnlyRounding() {
    if (mRoundingParams != null &&
        mRoundingParams.getRoundingMethod() == RoundingParams.RoundingMethod.BITMAP_ONLY) {
      // Bitmap rounding - either update the params or wrap the current drawable in a drawable
      // that will round it.
      for (int i = 0; i < mFadeDrawable.getNumberOfLayers(); i++) {
        Drawable layer = getLayerChildDrawable(i);
        if (layer instanceof Rounded) {
          Rounded rounded = (Rounded) layer;
          applyRoundingParams(rounded, mRoundingParams);
        } else if (layer != null) {
          // important: remove the child before wrapping it with a new parent!
          setLayerChildDrawable(i, mEmptyActualImageDrawable);
          Drawable roundedLayer = maybeApplyRoundingBitmapOnly(mRoundingParams, mResources, layer);
          setLayerChildDrawable(i, roundedLayer);
        }
      }
    } else {
      // No bitmap rounding requested - reset all layers so no rounding occurs.
      for (int i = 0; i < mFadeDrawable.getNumberOfLayers(); i++) {
        final Drawable layer = getLayerChildDrawable(i);
        if (layer instanceof Rounded) {
          resetRoundedDrawable((Rounded) layer);
        }
      }
    }
  }

  private static void resetRoundedDrawable(Rounded rounded) {
    rounded.setCircle(false);
    rounded.setRadius(0);
    rounded.setBorder(Color.TRANSPARENT, 0);
  }

  /**
   * Gets the rounding params.
   * @return rounding params
   */
  public RoundingParams getRoundingParams() {
    return mRoundingParams;
  }
}

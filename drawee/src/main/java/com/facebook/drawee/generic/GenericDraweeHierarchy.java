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

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import com.facebook.common.internal.Preconditions;
import com.facebook.drawee.drawable.DrawableParent;
import com.facebook.drawee.drawable.FadeDrawable;
import com.facebook.drawee.drawable.ForwardingDrawable;
import com.facebook.drawee.drawable.MatrixDrawable;
import com.facebook.drawee.drawable.ScaleTypeDrawable;
import com.facebook.drawee.interfaces.SettableDraweeHierarchy;

import static com.facebook.drawee.drawable.ScalingUtils.ScaleType;

/**
 * A SettableDraweeHierarchy that displays placeholder image until the actual image is set.
 * If provided, failure image will be used in case of failure (placeholder otherwise).
 * If provided, retry image will be used in case of failure when retrying is enabled.
 * If provided, progressbar will be displayed until fully loaded.
 * Each image can be displayed with a different scale type (or no scaling at all).
 * Fading between the layers is supported. Rounding is supported.
 *
 * <p>
 * Example hierarchy with a placeholder, retry, failure and the actual image:
 *  <pre>
 *  o RootDrawable (top level drawable)
 *  |
 *  +--o FadeDrawable
 *     |
 *     +--o ScaleTypeDrawable (placeholder branch, optional)
 *     |  |
 *     |  +--o Drawable (placeholder image)
 *     |
 *     +--o ScaleTypeDrawable (actual image branch)
 *     |  |
 *     |  +--o ForwardingDrawable (actual image wrapper)
 *     |     |
 *     |     +--o Drawable (actual image)
 *     |
 *     +--o null (progress bar branch, optional)
 *     |
 *     +--o Drawable (retry image branch, optional)
 *     |
 *     +--o ScaleTypeDrawable (failure image branch, optional)
 *        |
 *        +--o Drawable (failure image)
 *  </pre>
 *
 * <p>
 * Note:
 * <ul>
 * <li> RootDrawable and FadeDrawable are always created.
 * <li> All branches except the actual image branch are optional (placeholder, failure, retry,
 * progress bar). If some branch is not specified it won't be created. Index in FadeDrawable will
 * still be reserved though.
 * <li> If overlays and/or backgrounds are specified, they are added to the same fade drawable, and
 * are always being displayed.
 * <li> ScaleType and Matrix transformations will be added only if specified. If both are
 * unspecified, then the branch for that image is attached to FadeDrawable directly. Matrix
 * transformation is only supported for the actual image, and it is not recommended to be used.
 * <li> Rounding, if specified, is applied to all layers. Rounded drawable can either wrap
 * FadeDrawable, or if leaf rounding is specified, each leaf drawable will be rounded separately.
 * <li> A particular drawable instance should be used by only one DH. If more than one DH is being
 * built with the same builder, different drawable instances must be specified for each DH.
 * </ul>
 */
public class GenericDraweeHierarchy implements SettableDraweeHierarchy {

  private final Drawable mEmptyActualImageDrawable = new ColorDrawable(Color.TRANSPARENT);

  private final Resources mResources;
  private RoundingParams mRoundingParams;

  private final RootDrawable mTopLevelDrawable;
  private final FadeDrawable mFadeDrawable;
  private final ForwardingDrawable mActualImageWrapper;

  private final int mPlaceholderImageIndex;
  private final int mProgressBarImageIndex;
  private final int mActualImageIndex;
  private final int mRetryImageIndex;
  private final int mFailureImageIndex;

  GenericDraweeHierarchy(GenericDraweeHierarchyBuilder builder) {
    mResources = builder.getResources();
    mRoundingParams = builder.getRoundingParams();

    mActualImageWrapper = new ForwardingDrawable(mEmptyActualImageDrawable);

    int numBackgrounds = (builder.getBackgrounds() != null) ? builder.getBackgrounds().size() : 0;
    int numOverlays = (builder.getOverlays() != null) ? builder.getOverlays().size() : 0;
    numOverlays += (builder.getPressedStateOverlay() != null) ? 1 : 0;

    // layer indices and count
    int numLayers = 0;
    int backgroundsIndex = numLayers;
    numLayers += numBackgrounds;
    mPlaceholderImageIndex = numLayers++;
    mActualImageIndex = numLayers++;
    mProgressBarImageIndex = numLayers++;
    mRetryImageIndex = numLayers++;
    mFailureImageIndex = numLayers++;
    int overlaysIndex = numLayers;
    numLayers += numOverlays;

    // array of layers
    Drawable[] layers = new Drawable[numLayers];
    if (numBackgrounds > 0) {
      int index = 0;
      for (Drawable background : builder.getBackgrounds()) {
        layers[backgroundsIndex + index++] = buildBranch(background, null);
      }
    }
    layers[mPlaceholderImageIndex] = buildBranch(
        builder.getPlaceholderImage(),
        builder.getPlaceholderImageScaleType());
    layers[mActualImageIndex] = buildActualImageBranch(
        mActualImageWrapper,
        builder.getActualImageScaleType(),
        builder.getActualImageFocusPoint(),
        builder.getActualImageMatrix(),
        builder.getActualImageColorFilter());
    layers[mProgressBarImageIndex] = buildBranch(
        builder.getProgressBarImage(),
        builder.getProgressBarImageScaleType());
    layers[mRetryImageIndex] = buildBranch(
        builder.getRetryImage(),
        builder.getRetryImageScaleType());
    layers[mFailureImageIndex] = buildBranch(
        builder.getFailureImage(),
        builder.getFailureImageScaleType());
    if (numOverlays > 0) {
      int index = 0;
      if (builder.getOverlays() != null) {
        for (Drawable overlay : builder.getOverlays()) {
          layers[overlaysIndex + index++] = buildBranch(overlay, null);
        }
      }
      if (builder.getPressedStateOverlay() != null) {
        layers[overlaysIndex + index] = buildBranch(builder.getPressedStateOverlay(), null);
      }
    }

    // fade drawable composed of layers
    mFadeDrawable = new FadeDrawable(layers);
    mFadeDrawable.setTransitionDuration(builder.getFadeDuration());

    // rounded corners drawable (optional)
    Drawable maybeRoundedDrawable =
        WrappingUtils.maybeWrapWithRoundedOverlayColor(mFadeDrawable, mRoundingParams);

    // top-level drawable
    mTopLevelDrawable = new RootDrawable(maybeRoundedDrawable);
    mTopLevelDrawable.mutate();

    resetFade();
  }

  @Nullable
  private Drawable buildActualImageBranch(
      Drawable drawable,
      @Nullable ScaleType scaleType,
      @Nullable PointF focusPoint,
      @Nullable Matrix matrix,
      @Nullable ColorFilter colorFilter) {
    drawable.setColorFilter(colorFilter);
    drawable = WrappingUtils.maybeWrapWithScaleType(drawable, scaleType, focusPoint);
    drawable = WrappingUtils.maybeWrapWithMatrix(drawable, matrix);
    return drawable;
  }

  /** Applies scale type and rounding (both if specified). */
  @Nullable
  private Drawable buildBranch(@Nullable Drawable drawable, @Nullable ScaleType scaleType) {
    drawable = WrappingUtils.maybeApplyLeafRounding(drawable, mRoundingParams, mResources);
    drawable = WrappingUtils.maybeWrapWithScaleType(drawable, scaleType);
    return drawable;
  }

  private void resetActualImages() {
    mActualImageWrapper.setDrawable(mEmptyActualImageDrawable);
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
    Drawable progressBarDrawable = getParentDrawableAtIndex(mProgressBarImageIndex).getDrawable();
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
    drawable = WrappingUtils.maybeApplyLeafRounding(drawable, mRoundingParams, mResources);
    drawable.mutate();
    mActualImageWrapper.setDrawable(drawable);
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
    mTopLevelDrawable.setControllerOverlay(drawable);
  }

  // Helper methods for accessing layers

  /**
   * Gets the lowest parent drawable for the layer at the specified index.
   *
   * Following drawables are considered as parents: FadeDrawable, MatrixDrawable, ScaleTypeDrawable.
   * This is because those drawables are added automatically by the hierarchy (if specified),
   * whereas their children are created externally by the client code. When we need to change the
   * previously set drawable this is the parent whose child needs to be replaced.
   */
  private DrawableParent getParentDrawableAtIndex(int index) {
    DrawableParent parent = mFadeDrawable.getDrawableParentForIndex(index);
    if (parent.getDrawable() instanceof MatrixDrawable) {
      parent = (MatrixDrawable) parent.getDrawable();
    }
    if (parent.getDrawable() instanceof ScaleTypeDrawable) {
      parent = (ScaleTypeDrawable) parent.getDrawable();
    }
    return parent;
  }

  /**
   * Sets the drawable at the specified index while keeping the old scale type and rounding.
   * In case the given drawable is null, scale type gets cleared too.
   */
  private void setChildDrawableAtIndex(int index, @Nullable Drawable drawable) {
    if (drawable == null) {
      mFadeDrawable.setDrawable(index, null);
      return;
    }
    drawable = WrappingUtils.maybeApplyLeafRounding(drawable, mRoundingParams, mResources);
    getParentDrawableAtIndex(index).setDrawable(drawable);
  }

  /**
   * Gets the ScaleTypeDrawable at the specified index.
   * In case there is no child at the specified index, a NullPointerException is thrown.
   * In case there is a child, but the ScaleTypeDrawable does not exist,
   * the child will be wrapped with a new ScaleTypeDrawable.
   */
  private ScaleTypeDrawable getScaleTypeDrawableAtIndex(int index) {
    DrawableParent parent = getParentDrawableAtIndex(index);
    if (parent instanceof ScaleTypeDrawable) {
      return (ScaleTypeDrawable) parent;
    } else {
      return WrappingUtils.wrapChildWithScaleType(parent, ScaleType.FIT_XY);
    }
  }

  /**
   * Returns whether the given layer has a scale type drawable.
   */
  private boolean hasScaleTypeDrawableAtIndex(int index) {
    DrawableParent parent = getParentDrawableAtIndex(index);
    return (parent instanceof ScaleTypeDrawable);
  }

  // Mutability

  /** Sets the fade duration. */
  public void setFadeDuration(int durationMs) {
    mFadeDrawable.setTransitionDuration(durationMs);
  }

  /** Sets the actual image focus point. */
  public void setActualImageFocusPoint(PointF focusPoint) {
    Preconditions.checkNotNull(focusPoint);
    getScaleTypeDrawableAtIndex(mActualImageIndex).setFocusPoint(focusPoint);
  }

  /** Sets the actual image scale type. */
  public void setActualImageScaleType(ScaleType scaleType) {
    Preconditions.checkNotNull(scaleType);
    getScaleTypeDrawableAtIndex(mActualImageIndex).setScaleType(scaleType);
  }

  public @Nullable ScaleType getActualImageScaleType() {
    if (!hasScaleTypeDrawableAtIndex(mActualImageIndex)) {
      return null;
    }
    return getScaleTypeDrawableAtIndex(mActualImageIndex).getScaleType();
  }

  /** Sets the color filter to be applied on the actual image. */
  public void setActualImageColorFilter(ColorFilter colorfilter) {
    mActualImageWrapper.setColorFilter(colorfilter);
  }

  /** Gets the non-cropped post-scaling bounds of the actual image. */
  public void getActualImageBounds(RectF outBounds) {
    mActualImageWrapper.getTransformedBounds(outBounds);
  }

  /** Sets a new placeholder drawable with old scale type. */
  public void setPlaceholderImage(@Nullable Drawable drawable) {
    setChildDrawableAtIndex(mPlaceholderImageIndex, drawable);
  }

  /** Sets a new placeholder drawable with scale type. */
  public void setPlaceholderImage(Drawable drawable, ScaleType scaleType) {
    setChildDrawableAtIndex(mPlaceholderImageIndex, drawable);
    getScaleTypeDrawableAtIndex(mPlaceholderImageIndex).setScaleType(scaleType);

  }

  /** Sets the placeholder image focus point. */
  public void setPlaceholderImageFocusPoint(PointF focusPoint) {
    Preconditions.checkNotNull(focusPoint);
    getScaleTypeDrawableAtIndex(mPlaceholderImageIndex).setFocusPoint(focusPoint);
  }

  /**
   * Sets a new placeholder drawable with old scale type.
   *
   * @param resourceId an identifier of an Android drawable or color resource.
   */
  public void setPlaceholderImage(int resourceId) {
    setPlaceholderImage(mResources.getDrawable(resourceId));
  }

  /** Sets a new failure drawable with old scale type. */
  public void setFailureImage(@Nullable Drawable drawable) {
    setChildDrawableAtIndex(mFailureImageIndex, drawable);
  }

  /** Sets a new failure drawable with scale type. */
  public void setFailureImage(Drawable drawable, ScaleType scaleType) {
    setChildDrawableAtIndex(mFailureImageIndex, drawable);
    getScaleTypeDrawableAtIndex(mFailureImageIndex).setScaleType(scaleType);
  }

  /** Sets a new retry drawable with old scale type. */
  public void setRetryImage(@Nullable Drawable drawable) {
    setChildDrawableAtIndex(mRetryImageIndex, drawable);
  }

  /** Sets a new retry drawable with scale type. */
  public void setRetryImage(Drawable drawable, ScaleType scaleType) {
    setChildDrawableAtIndex(mRetryImageIndex, drawable);
    getScaleTypeDrawableAtIndex(mRetryImageIndex).setScaleType(scaleType);
  }

  /** Sets a new progress bar drawable with old scale type. */
  public void setProgressBarImage(@Nullable Drawable drawable) {
    setChildDrawableAtIndex(mProgressBarImageIndex, drawable);
  }

  /** Sets a new progress bar drawable with scale type. */
  public void setProgressBarImage(Drawable drawable, ScaleType scaleType) {
    setChildDrawableAtIndex(mProgressBarImageIndex, drawable);
    getScaleTypeDrawableAtIndex(mProgressBarImageIndex).setScaleType(scaleType);
  }

  /** Sets the rounding params. */
  public void setRoundingParams(RoundingParams roundingParams) {
    mRoundingParams = roundingParams;
    WrappingUtils.updateOverlayColorRounding(mTopLevelDrawable, mRoundingParams);
    for (int i = 0; i < mFadeDrawable.getNumberOfLayers(); i++) {
      WrappingUtils.updateLeafRounding(getParentDrawableAtIndex(i), mRoundingParams, mResources);
    }
  }

  /** Gets the rounding params. */
  public RoundingParams getRoundingParams() {
    return mRoundingParams;
  }
}

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

  private final Drawable mEmptyActualImageDrawable = new ColorDrawable(Color.TRANSPARENT);

  private final Resources mResources;

  private final RootDrawable mTopLevelDrawable;
  private final FadeDrawable mFadeDrawable;
  private final ForwardingDrawable mActualImageSettableDrawable;

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
    if (placeholderImageBranch != null) {
      placeholderImageBranch = WrappingUtils.maybeApplyLeafRounding(
          placeholderImageBranch,
          mRoundingParams,
          mResources);
      placeholderImageBranch = WrappingUtils.maybeWrapWithScaleType(
          placeholderImageBranch,
          builder.getPlaceholderImageScaleType());
    }
    mPlaceholderImageIndex = numLayers++;

    // actual image branch
    Drawable actualImageBranch;
    mActualImageSettableDrawable = new ForwardingDrawable(mEmptyActualImageDrawable);
    actualImageBranch = mActualImageSettableDrawable;
    actualImageBranch.setColorFilter(builder.getActualImageColorFilter());
    actualImageBranch = WrappingUtils.maybeWrapWithScaleType(
        actualImageBranch,
        builder.getActualImageScaleType(),
        builder.getActualImageFocusPoint());
    actualImageBranch = WrappingUtils.maybeWrapWithMatrix(
        actualImageBranch,
        builder.getActualImageMatrix());
    mActualImageIndex = numLayers++;

    // progressBar image branch
    Drawable progressBarImageBranch = builder.getProgressBarImage();
    mProgressBarImageIndex = numLayers++;
    if (progressBarImageBranch != null) {
      progressBarImageBranch = WrappingUtils.maybeWrapWithScaleType(
          progressBarImageBranch,
          builder.getProgressBarImageScaleType());
    }

    // retry image branch
    Drawable retryImageBranch = builder.getRetryImage();
    mRetryImageIndex = numLayers++;
    if (retryImageBranch != null) {
      retryImageBranch = WrappingUtils.maybeWrapWithScaleType(
          retryImageBranch,
          builder.getRetryImageScaleType());
    }

    // failure image branch
    Drawable failureImageBranch = builder.getFailureImage();
    mFailureImageIndex = numLayers++;
    if (failureImageBranch != null) {
      failureImageBranch = WrappingUtils.maybeWrapWithScaleType(
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
            WrappingUtils.maybeApplyLeafRounding(background, mRoundingParams, mResources);
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
    layers[mControllerOverlayIndex] = null;

    // fade drawable composed of branches
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
    Drawable progressBarDrawable = getLayerParentDrawable(mProgressBarImageIndex).getDrawable();
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
    mFadeDrawable.setDrawable(mControllerOverlayIndex, drawable);
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
  private DrawableParent getLayerParentDrawable(int index) {
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
   * Sets the drawable at the specified index and keeps the old scale type.
   * In case the given drawable is null, scale type gets cleared too.
   */
  private void setLayerChildDrawable(int index, @Nullable Drawable drawable) {
    if (drawable == null) {
      mFadeDrawable.setDrawable(index, null);
      return;
    }
    drawable = WrappingUtils.maybeApplyLeafRounding(drawable, mRoundingParams, mResources);
    getLayerParentDrawable(index).setDrawable(drawable);
  }

  /**
   * Gets the ScaleTypeDrawable at the specified index.
   * In case there is no child at the specified index, a NullPointerException is thrown.
   * In case there is a child, but the ScaleTypeDrawable does not exist,
   * the child will be wrapped with a new ScaleTypeDrawable.
   */
  private ScaleTypeDrawable getLayerScaleTypeDrawable(int index) {
    DrawableParent parent = getLayerParentDrawable(index);
    if (parent instanceof ScaleTypeDrawable) {
      return (ScaleTypeDrawable) parent;
    } else {
      return WrappingUtils.wrapChildWithScaleType(parent, ScaleType.FIT_XY);
    }
  }

  // Mutability

  /** Sets the fade duration. */
  public void setFadeDuration(int durationMs) {
    mFadeDrawable.setTransitionDuration(durationMs);
  }

  /** Sets the actual image focus point. */
  public void setActualImageFocusPoint(PointF focusPoint) {
    Preconditions.checkNotNull(focusPoint);
    getLayerScaleTypeDrawable(mActualImageIndex).setFocusPoint(focusPoint);
  }

  /** Sets the actual image scale type. */
  public void setActualImageScaleType(ScaleType scaleType) {
    Preconditions.checkNotNull(scaleType);
    getLayerScaleTypeDrawable(mActualImageIndex).setScaleType(scaleType);
  }

  /** Sets the color filter to be applied on the actual image. */
  public void setActualImageColorFilter(ColorFilter colorfilter) {
    mActualImageSettableDrawable.setColorFilter(colorfilter);
  }

  /** Gets the non-cropped post-scaling bounds of the actual image. */
  public void getActualImageBounds(RectF outBounds) {
    mActualImageSettableDrawable.getTransformedBounds(outBounds);
  }

  /** Sets a new placeholder drawable with old scale type. */
  public void setPlaceholderImage(@Nullable Drawable drawable) {
    setLayerChildDrawable(mPlaceholderImageIndex, drawable);
  }

  /** Sets a new placeholder drawable with scale type. */
  public void setPlaceholderImage(Drawable drawable, ScaleType scaleType) {
    setLayerChildDrawable(mPlaceholderImageIndex, drawable);
    getLayerScaleTypeDrawable(mPlaceholderImageIndex).setScaleType(scaleType);

  }

  /** Sets the placeholder image focus point. */
  public void setPlaceholderImageFocusPoint(PointF focusPoint) {
    Preconditions.checkNotNull(focusPoint);
    getLayerScaleTypeDrawable(mPlaceholderImageIndex).setFocusPoint(focusPoint);
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
    setLayerChildDrawable(mFailureImageIndex, drawable);
  }

  /** Sets a new failure drawable with scale type. */
  public void setFailureImage(Drawable drawable, ScaleType scaleType) {
    setLayerChildDrawable(mFailureImageIndex, drawable);
    getLayerScaleTypeDrawable(mFailureImageIndex).setScaleType(scaleType);
  }

  /** Sets a new retry drawable with old scale type. */
  public void setRetryImage(@Nullable Drawable drawable) {
    setLayerChildDrawable(mRetryImageIndex, drawable);
  }

  /** Sets a new retry drawable with scale type. */
  public void setRetryImage(Drawable drawable, ScaleType scaleType) {
    setLayerChildDrawable(mRetryImageIndex, drawable);
    getLayerScaleTypeDrawable(mRetryImageIndex).setScaleType(scaleType);
  }

  /** Sets a new progress bar drawable with old scale type. */
  public void setProgressBarImage(@Nullable Drawable drawable) {
    setLayerChildDrawable(mProgressBarImageIndex, drawable);
  }

  /** Sets a new progress bar drawable with scale type. */
  public void setProgressBarImage(Drawable drawable, ScaleType scaleType) {
    setLayerChildDrawable(mProgressBarImageIndex, drawable);
    getLayerScaleTypeDrawable(mProgressBarImageIndex).setScaleType(scaleType);
  }

  /** Sets the rounding params. */
  public void setRoundingParams(RoundingParams roundingParams) {
    mRoundingParams = roundingParams;
    WrappingUtils.updateOverlayColorRounding(mTopLevelDrawable, mRoundingParams);
    for (int i = 0; i < mFadeDrawable.getNumberOfLayers(); i++) {
      WrappingUtils.updateLeafRounding(getLayerParentDrawable(i), mRoundingParams, mResources);
    }
  }

  /** Gets the rounding params. */
  public RoundingParams getRoundingParams() {
    return mRoundingParams;
  }
}

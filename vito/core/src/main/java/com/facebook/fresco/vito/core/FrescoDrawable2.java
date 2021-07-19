/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core;

import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import androidx.annotation.Nullable;
import com.facebook.drawee.components.DeferredReleaser;
import com.facebook.drawee.drawable.FadeDrawable;
import com.facebook.drawee.drawable.ScaleTypeDrawable;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.drawable.TransformAwareDrawable;
import com.facebook.drawee.drawable.TransformCallback;
import com.facebook.drawee.drawable.VisibilityCallback;
import com.facebook.fresco.vito.listener.ImageListener;
import com.facebook.infer.annotation.Nullsafe;
import java.io.Closeable;

@Nullsafe(Nullsafe.Mode.STRICT)
public abstract class FrescoDrawable2 extends FadeDrawable
    implements Drawable.Callback,
        TransformCallback,
        TransformAwareDrawable,
        Closeable,
        DeferredReleaser.Releasable {

  private static final int LAYER_COUNT = 4;

  private static final int PLACEHOLDER_DRAWABLE_INDEX = 0;
  public static final int IMAGE_DRAWABLE_INDEX = 1;
  private static final int PROGRESS_DRAWABLE_INDEX = 2;
  private static final int OVERLAY_DRAWABLE_INDEX = 3;

  private @Nullable Rect mViewportDimensions;

  private @Nullable VisibilityCallback mVisibilityCallback;

  public FrescoDrawable2() {
    super(new Drawable[LAYER_COUNT], false, IMAGE_DRAWABLE_INDEX);
  }

  public boolean hasImage() {
    return getDrawable(IMAGE_DRAWABLE_INDEX) != null;
  }

  public @Nullable Drawable setOverlayDrawable(@Nullable Drawable drawable) {
    return setDrawable(OVERLAY_DRAWABLE_INDEX, drawable);
  }

  public @Nullable Drawable setProgressDrawable(@Nullable Drawable drawable) {
    return setDrawable(PROGRESS_DRAWABLE_INDEX, drawable);
  }

  public void setProgress(float progress) {
    Drawable progressBarDrawable = getDrawable(PROGRESS_DRAWABLE_INDEX);
    if (progressBarDrawable == null) {
      return;
    }
    // display progressbar when not fully loaded, hide otherwise
    if (progress >= 0.999f) {
      maybeStopAnimation(progressBarDrawable);
    } else {
      maybeStartAnimation(progressBarDrawable);
    }
    // set drawable level, scaled to [0, 10000] per drawable specification
    progressBarDrawable.setLevel(Math.round(progress * 10000));
  }

  public @Nullable Drawable setPlaceholderDrawable(@Nullable Drawable drawable) {
    return setDrawable(PLACEHOLDER_DRAWABLE_INDEX, drawable);
  }

  public void fadeInImage(int durationMs) {
    setTransitionDuration(durationMs);
    beginBatchMode();
    fadeOutLayer(PLACEHOLDER_DRAWABLE_INDEX);
    fadeOutLayer(PROGRESS_DRAWABLE_INDEX);
    fadeInLayer(IMAGE_DRAWABLE_INDEX);
    endBatchMode();
  }

  public void showImageImmediately() {
    beginBatchMode();
    hideLayerImmediately(PLACEHOLDER_DRAWABLE_INDEX);
    hideLayerImmediately(PROGRESS_DRAWABLE_INDEX);
    showLayerImmediately(IMAGE_DRAWABLE_INDEX);
    endBatchMode();
  }

  public void showOverlayImmediately() {
    showLayerImmediately(OVERLAY_DRAWABLE_INDEX);
  }

  public void showProgressImmediately() {
    showLayerImmediately(PROGRESS_DRAWABLE_INDEX);
  }

  @Override
  public void close() {
    maybeStopAnimation(getDrawable(PLACEHOLDER_DRAWABLE_INDEX));
    for (int i = 0; i < LAYER_COUNT; i++) {
      setDrawable(i, null);
    }
  }

  @Override
  public boolean setVisible(boolean visible, boolean restart) {
    if (mVisibilityCallback != null) {
      mVisibilityCallback.onVisibilityChange(visible);
    }
    return super.setVisible(visible, restart);
  }

  public void setVisibilityCallback(@Nullable VisibilityCallback visibilityCallback) {
    mVisibilityCallback = visibilityCallback;
  }

  public @Nullable Drawable getOverlayDrawable() {
    return getDrawable(OVERLAY_DRAWABLE_INDEX);
  }

  @Nullable
  public Rect getViewportDimensions() {
    return mViewportDimensions;
  }

  public void setViewportDimensions(@Nullable Rect viewportDimensions) {
    mViewportDimensions = viewportDimensions;
  }

  public @Nullable ScalingUtils.ScaleType getActualImageScaleType() {
    Drawable actual = getDrawable(IMAGE_DRAWABLE_INDEX);
    if (!(actual instanceof ScaleTypeDrawable)) return null;

    return ((ScaleTypeDrawable) actual).getScaleType();
  }

  public @Nullable PointF getActualImageFocusPoint() {
    Drawable actual = getDrawable(IMAGE_DRAWABLE_INDEX);
    if (!(actual instanceof ScaleTypeDrawable)) return null;

    return ((ScaleTypeDrawable) actual).getFocusPoint();
  }

  /** @return the width of the underlying actual image or -1 if unset */
  public abstract int getActualImageWidthPx();

  /** @return the width of the underlying actual image or -1 if unset */
  public abstract int getActualImageHeightPx();

  public abstract ScaleTypeDrawable getActualImageWrapper();

  public abstract @Nullable Drawable getActualImageDrawable();

  public abstract boolean isFetchSubmitted();

  public abstract void setImageRequest(@Nullable VitoImageRequest imageRequest);

  public abstract void setCallerContext(@Nullable Object callerContext);

  public abstract @Nullable Object getCallerContext();

  public abstract void setImageListener(@Nullable ImageListener imageListener);

  public abstract @Nullable ImageListener getImageListener();

  public abstract @Nullable VitoImageRequest getImageRequest();

  public abstract long getImageId();

  public abstract void cancelReleaseDelayed();

  public abstract void cancelReleaseNextFrame();

  public abstract @Nullable Object getExtras();

  public abstract void setExtras(@Nullable Object extras);

  public abstract VitoImagePerfListener getImagePerfListener();

  private static void maybeStopAnimation(@Nullable Drawable drawable) {
    if (drawable instanceof Animatable) {
      ((Animatable) drawable).stop();
    }
  }

  private static void maybeStartAnimation(@Nullable Drawable drawable) {
    if (drawable instanceof Animatable) {
      ((Animatable) drawable).start();
    }
  }
}

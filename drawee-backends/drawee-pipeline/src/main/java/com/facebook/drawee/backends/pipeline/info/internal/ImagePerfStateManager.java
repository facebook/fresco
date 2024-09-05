/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.backends.pipeline.info.internal;

import androidx.annotation.VisibleForTesting;
import com.facebook.common.time.MonotonicClock;
import com.facebook.drawee.drawable.VisibilityCallback;
import com.facebook.fresco.ui.common.BaseControllerListener2;
import com.facebook.fresco.ui.common.ControllerListener2;
import com.facebook.fresco.ui.common.DimensionsInfo;
import com.facebook.fresco.ui.common.ImageLoadStatus;
import com.facebook.fresco.ui.common.ImagePerfNotifier;
import com.facebook.fresco.ui.common.ImagePerfNotifierHolder;
import com.facebook.fresco.ui.common.ImagePerfState;
import com.facebook.fresco.ui.common.OnDrawControllerListener;
import com.facebook.fresco.ui.common.VisibilityAware;
import com.facebook.fresco.ui.common.VisibilityState;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.infer.annotation.Nullsafe;
import java.io.Closeable;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class ImagePerfStateManager extends BaseControllerListener2<ImageInfo>
    implements ImagePerfNotifierHolder,
        OnDrawControllerListener<ImageInfo>,
        Closeable,
        VisibilityAware,
        VisibilityCallback {

  private final MonotonicClock mClock;
  private final ImagePerfState mImagePerfState;
  private final ImagePerfNotifier mImagePerfNotifier;

  private @Nullable ImagePerfNotifier mLocalImagePerfNotifier = null;

  private final boolean mReportVisibleOnSubmitAndRelease;

  public ImagePerfStateManager(
      MonotonicClock clock,
      ImagePerfState imagePerfState,
      ImagePerfNotifier globalImagePerfNotifier) {
    this(clock, imagePerfState, globalImagePerfNotifier, true);
  }

  public ImagePerfStateManager(
      MonotonicClock clock,
      ImagePerfState imagePerfState,
      ImagePerfNotifier globalImagePerfNotifier,
      boolean reportVisibleOnSubmitAndRelease) {
    mClock = clock;
    mImagePerfState = imagePerfState;
    mImagePerfNotifier = globalImagePerfNotifier;
    mReportVisibleOnSubmitAndRelease = reportVisibleOnSubmitAndRelease;
  }

  @Override
  public void setImagePerfNotifier(@Nullable ImagePerfNotifier imagePerfNotifier) {
    mLocalImagePerfNotifier = imagePerfNotifier;
  }

  @Override
  public void onSubmit(
      String id, @Nullable Object callerContext, @Nullable ControllerListener2.Extras extraData) {
    final long now = mClock.now();

    ImagePerfState state = mImagePerfState;
    state.resetPointsTimestamps();

    state.setControllerSubmitTimeMs(now);
    state.setControllerId(id);
    state.setCallerContext(callerContext);

    state.setExtraData(extraData);

    updateStatus(state, ImageLoadStatus.REQUESTED);
    if (mReportVisibleOnSubmitAndRelease) {
      reportViewVisible(state, now);
    }
  }

  @Override
  public void onIntermediateImageSet(String id, @Nullable ImageInfo imageInfo) {
    final long now = mClock.now();

    ImagePerfState state = mImagePerfState;

    state.setControllerIntermediateImageSetTimeMs(now);
    state.setControllerId(id);
    state.setImageInfo(imageInfo);

    updateStatus(state, ImageLoadStatus.INTERMEDIATE_AVAILABLE);
  }

  @Override
  public void onFinalImageSet(
      String id, @Nullable ImageInfo imageInfo, @Nullable ControllerListener2.Extras extraData) {
    final long now = mClock.now();

    ImagePerfState state = mImagePerfState;

    state.setExtraData(extraData);

    state.setControllerFinalImageSetTimeMs(now);
    state.setImageRequestEndTimeMs(now);
    state.setControllerId(id);
    state.setImageInfo(imageInfo);

    updateStatus(state, ImageLoadStatus.SUCCESS);
  }

  @Override
  public void onFailure(
      String id, @Nullable Throwable throwable, @Nullable ControllerListener2.Extras extras) {
    final long now = mClock.now();

    ImagePerfState state = mImagePerfState;

    state.setExtraData(extras);

    state.setControllerFailureTimeMs(now);
    state.setControllerId(id);
    state.setErrorThrowable(throwable);

    updateStatus(state, ImageLoadStatus.ERROR);

    reportViewInvisible(state, now);
  }

  @Override
  public void onRelease(String id, @Nullable ControllerListener2.Extras extras) {
    final long now = mClock.now();

    ImagePerfState state = mImagePerfState;

    state.setExtraData(extras);
    state.setControllerId(id);

    updateStatus(state, ImageLoadStatus.RELEASED);

    if (mReportVisibleOnSubmitAndRelease) {
      reportViewInvisible(state, now);
    }
  }

  @Override
  public void onImageDrawn(String id, ImageInfo info, DimensionsInfo dimensionsInfo) {
    ImagePerfState state = mImagePerfState;

    state.setControllerId(id);
    state.setDimensionsInfo(dimensionsInfo);
  }

  @VisibleForTesting
  public void reportViewVisible(ImagePerfState state, long now) {
    state.setVisible(true);
    state.setVisibilityEventTimeMs(now);

    updateVisibility(state, VisibilityState.VISIBLE);
  }

  public void resetState() {
    mImagePerfState.reset();
  }

  @Override
  public void close() {
    resetState();
  }

  @VisibleForTesting
  private void reportViewInvisible(ImagePerfState state, long time) {
    state.setVisible(false);
    state.setInvisibilityEventTimeMs(time);

    updateVisibility(state, VisibilityState.INVISIBLE);
  }

  private void updateStatus(ImagePerfState state, ImageLoadStatus imageLoadStatus) {
    state.setImageLoadStatus(imageLoadStatus);

    mImagePerfNotifier.notifyStatusUpdated(state, imageLoadStatus);
    ImagePerfNotifier localImagePerfNotifier = mLocalImagePerfNotifier;
    if (localImagePerfNotifier != null) {
      localImagePerfNotifier.notifyStatusUpdated(state, imageLoadStatus);
    }
  }

  private void updateVisibility(ImagePerfState state, VisibilityState visibilityState) {
    mImagePerfNotifier.notifyVisibilityUpdated(state, visibilityState);
    ImagePerfNotifier localImagePerfNotifier = mLocalImagePerfNotifier;
    if (localImagePerfNotifier != null) {
      localImagePerfNotifier.notifyVisibilityUpdated(state, visibilityState);
    }
  }

  @Override
  public void onEmptyEvent(@Nullable Object callerContext) {
    ImagePerfState state = mImagePerfState;
    state.setImageLoadStatus(ImageLoadStatus.EMPTY_EVENT);
    mImagePerfNotifier.notifyStatusUpdated(state, ImageLoadStatus.EMPTY_EVENT);
    ImagePerfNotifier localImagePerfNotifier = mLocalImagePerfNotifier;
    if (localImagePerfNotifier != null) {
      localImagePerfNotifier.notifyStatusUpdated(state, ImageLoadStatus.EMPTY_EVENT);
    }
  }

  @Override
  public void reportVisible(boolean visible) {
    onVisibilityChange(visible);
  }

  @Override
  public void onVisibilityChange(boolean visible) {
    if (visible) {
      reportViewVisible(mImagePerfState, mClock.now());
    } else {
      reportViewInvisible(mImagePerfState, mClock.now());
    }
  }

  @Override
  public void onDraw() {
    // No-op
  }
}

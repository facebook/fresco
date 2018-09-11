/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.drawee.backends.pipeline.info.internal;

import android.graphics.drawable.Animatable;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.time.MonotonicClock;
import com.facebook.drawee.backends.pipeline.info.ImageLoadStatus;
import com.facebook.drawee.backends.pipeline.info.ImagePerfMonitor;
import com.facebook.drawee.backends.pipeline.info.ImagePerfState;
import com.facebook.drawee.backends.pipeline.info.VisibilityState;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.imagepipeline.image.ImageInfo;
import javax.annotation.Nullable;

public class ImagePerfControllerListener extends BaseControllerListener<ImageInfo> {

  private final MonotonicClock mClock;
  private final ImagePerfState mImagePerfState;
  private final ImagePerfMonitor mImagePerfMonitor;

  public ImagePerfControllerListener(
      MonotonicClock clock, ImagePerfState imagePerfState, ImagePerfMonitor imagePerfMonitor) {
    mClock = clock;
    mImagePerfState = imagePerfState;
    mImagePerfMonitor = imagePerfMonitor;
  }

  @Override
  public void onSubmit(String id, Object callerContext) {
    final long now = mClock.now();

    mImagePerfState.setControllerSubmitTimeMs(now);
    mImagePerfState.setControllerId(id);
    mImagePerfState.setCallerContext(callerContext);

    mImagePerfMonitor.notifyStatusUpdated(mImagePerfState, ImageLoadStatus.REQUESTED);
    reportViewVisible(now);
  }

  @Override
  public void onIntermediateImageSet(String id, @Nullable ImageInfo imageInfo) {
    final long now = mClock.now();

    mImagePerfState.setControllerIntermediateImageSetTimeMs(now);
    mImagePerfState.setControllerId(id);
    mImagePerfState.setImageInfo(imageInfo);

    mImagePerfMonitor.notifyStatusUpdated(mImagePerfState, ImageLoadStatus.INTERMEDIATE_AVAILABLE);
  }

  @Override
  public void onFinalImageSet(
      String id, @Nullable ImageInfo imageInfo, @Nullable Animatable animatable) {
    final long now = mClock.now();

    mImagePerfState.setControllerFinalImageSetTimeMs(now);
    mImagePerfState.setImageRequestEndTimeMs(now);
    mImagePerfState.setControllerId(id);
    mImagePerfState.setImageInfo(imageInfo);

    mImagePerfMonitor.notifyStatusUpdated(mImagePerfState, ImageLoadStatus.SUCCESS);
  }

  @Override
  public void onFailure(String id, Throwable throwable) {
    final long now = mClock.now();

    mImagePerfState.setControllerFailureTimeMs(now);
    mImagePerfState.setControllerId(id);

    mImagePerfMonitor.notifyStatusUpdated(mImagePerfState, ImageLoadStatus.ERROR);

    reportViewInvisible(now);
  }

  @Override
  public void onRelease(String id) {
    super.onRelease(id);
    final long now = mClock.now();

    int lastImageLoadStatus = mImagePerfState.getImageLoadStatus();
    if (lastImageLoadStatus != ImageLoadStatus.SUCCESS
        && lastImageLoadStatus != ImageLoadStatus.ERROR) {
      mImagePerfState.setControllerCancelTimeMs(now);
      mImagePerfState.setControllerId(id);
      // The image request was canceled
      mImagePerfMonitor.notifyStatusUpdated(mImagePerfState, ImageLoadStatus.CANCELED);
    }

    reportViewInvisible(now);
  }

  @VisibleForTesting
  public void reportViewVisible(long now) {
    mImagePerfState.setVisible(true);
    mImagePerfState.setVisibilityEventTimeMs(now);

    mImagePerfMonitor.notifyListenersOfVisibilityStateUpdate(
        mImagePerfState, VisibilityState.VISIBLE);
  }

  @VisibleForTesting
  private void reportViewInvisible(long time) {
    mImagePerfState.setVisible(false);
    mImagePerfState.setInvisibilityEventTimeMs(time);

    mImagePerfMonitor.notifyListenersOfVisibilityStateUpdate(
        mImagePerfState, VisibilityState.INVISIBLE);
  }
}

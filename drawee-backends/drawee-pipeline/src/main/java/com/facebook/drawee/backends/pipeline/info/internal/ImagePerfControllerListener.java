/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.drawee.backends.pipeline.info.internal;

import android.graphics.drawable.Animatable;
import com.facebook.common.time.MonotonicClock;
import com.facebook.drawee.backends.pipeline.info.ImageLoadStatus;
import com.facebook.drawee.backends.pipeline.info.ImagePerfMonitor;
import com.facebook.drawee.backends.pipeline.info.ImagePerfState;
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
    mImagePerfState.setControllerSubmitTimeMs(mClock.now());

    mImagePerfState.setRequestId(id);
    mImagePerfState.setCallerContext(callerContext);

    mImagePerfMonitor.notifyListeners(mImagePerfState, ImageLoadStatus.REQUESTED);
  }

  @Override
  public void onIntermediateImageSet(String id, @Nullable ImageInfo imageInfo) {
    mImagePerfState.setControllerIntermediateImageSetTimeMs(mClock.now());

    mImagePerfState.setRequestId(id);
    mImagePerfState.setImageInfo(imageInfo);

    mImagePerfMonitor.notifyListeners(mImagePerfState, ImageLoadStatus.INTERMEDIATE_AVAILABLE);
  }

  @Override
  public void onFinalImageSet(
      String id, @Nullable ImageInfo imageInfo, @Nullable Animatable animatable) {
    mImagePerfState.setControllerFinalImageSetTimeMs(mClock.now());

    mImagePerfState.setRequestId(id);
    mImagePerfState.setImageInfo(imageInfo);
    mImagePerfState.setSuccessful(true);

    mImagePerfMonitor.notifyListeners(mImagePerfState, ImageLoadStatus.SUCCESS);
  }

  @Override
  public void onFailure(String id, Throwable throwable) {
    mImagePerfState.setControllerFailureTimeMs(mClock.now());

    mImagePerfState.setRequestId(id);
    mImagePerfState.setSuccessful(false);

    mImagePerfMonitor.notifyListeners(mImagePerfState, ImageLoadStatus.ERROR);
  }
}

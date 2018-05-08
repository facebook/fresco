/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.drawee.backends.pipeline.info.internal;

import com.facebook.common.time.MonotonicClock;
import com.facebook.drawee.backends.pipeline.info.ImageLoadStatus;
import com.facebook.drawee.backends.pipeline.info.ImagePerfMonitor;
import com.facebook.drawee.backends.pipeline.info.ImagePerfState;
import com.facebook.imagepipeline.listener.BaseRequestListener;
import com.facebook.imagepipeline.request.ImageRequest;

public class ImagePerfRequestListener extends BaseRequestListener {

  private final MonotonicClock mClock;
  private final ImagePerfState mImagePerfState;
  private final ImagePerfMonitor mImagePerfMonitor;

  public ImagePerfRequestListener(
      MonotonicClock monotonicClock,
      ImagePerfState imagePerfState,
      ImagePerfMonitor imagePerfMonitor) {
    mClock = monotonicClock;
    mImagePerfState = imagePerfState;
    mImagePerfMonitor = imagePerfMonitor;
  }

  @Override
  public void onRequestStart(
      ImageRequest request, Object callerContext, String requestId, boolean isPrefetch) {
    mImagePerfState.setImageRequestStartTimeMs(mClock.now());

    mImagePerfState.setImageRequest(request);
    mImagePerfState.setCallerContext(callerContext);
    mImagePerfState.setRequestId(requestId);
    mImagePerfState.setPrefetch(isPrefetch);

    mImagePerfMonitor.notifyListeners(mImagePerfState, ImageLoadStatus.REQUESTED);
  }

  @Override
  public void onRequestSuccess(ImageRequest request, String requestId, boolean isPrefetch) {
    mImagePerfState.setImageRequestEndTimeMs(mClock.now());

    mImagePerfState.setImageRequest(request);
    mImagePerfState.setRequestId(requestId);
    mImagePerfState.setPrefetch(isPrefetch);
    mImagePerfState.setSuccessful(true);

    mImagePerfMonitor.notifyListeners(mImagePerfState, ImageLoadStatus.SUCCESS);
  }

  @Override
  public void onRequestFailure(
      ImageRequest request, String requestId, Throwable throwable, boolean isPrefetch) {
    mImagePerfState.setImageRequestEndTimeMs(mClock.now());

    mImagePerfState.setImageRequest(request);
    mImagePerfState.setRequestId(requestId);
    mImagePerfState.setPrefetch(isPrefetch);
    mImagePerfState.setSuccessful(false);

    mImagePerfMonitor.notifyListeners(mImagePerfState, ImageLoadStatus.ERROR);
  }

  @Override
  public void onRequestCancellation(String requestId) {
    mImagePerfState.setImageRequestEndTimeMs(mClock.now());

    mImagePerfState.setRequestId(requestId);
    mImagePerfState.setCanceled(true);

    mImagePerfMonitor.notifyListeners(mImagePerfState, ImageLoadStatus.CANCELED);
  }
}

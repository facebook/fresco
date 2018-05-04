/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.drawee.backends.pipeline.info;

import static com.facebook.drawee.backends.pipeline.info.ImagePerfData.UNSET;

import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.request.ImageRequest;
import javax.annotation.Nullable;

public class ImagePerfState {

  // General image metadata
  private @Nullable String mRequestId;
  private @Nullable ImageRequest mImageRequest;
  private @Nullable Object mCallerContext;
  private @Nullable ImageInfo mImageInfo;

  // Controller timings
  private long mControllerSubmitTimeMs = UNSET;
  private long mControllerFinalImageSetTimeMs = UNSET;
  private long mControllerFailureTimeMs = UNSET;

  // Image request timings
  private long mImageRequestStartTimeMs = UNSET;
  private long mImageRequestEndTimeMs = UNSET;

  // Image pipeline information
  private @ImageOrigin int mImageOrigin = ImageOrigin.UNKNOWN;
  private boolean mIsCanceled;
  private boolean mIsSuccessful;
  private boolean mIsPrefetch;

  public void setRequestId(@Nullable String requestId) {
    mRequestId = requestId;
  }

  public void setImageRequest(@Nullable ImageRequest imageRequest) {
    mImageRequest = imageRequest;
  }

  public void setCallerContext(@Nullable Object callerContext) {
    mCallerContext = callerContext;
  }

  public void setControllerSubmitTimeMs(long controllerSubmitTimeMs) {
    mControllerSubmitTimeMs = controllerSubmitTimeMs;
  }

  public void setControllerFinalImageSetTimeMs(long controllerFinalImageSetTimeMs) {
    mControllerFinalImageSetTimeMs = controllerFinalImageSetTimeMs;
  }

  public void setControllerFailureTimeMs(long controllerFailureTimeMs) {
    mControllerFailureTimeMs = controllerFailureTimeMs;
  }

  public void setImageRequestStartTimeMs(long imageRequestStartTimeMs) {
    mImageRequestStartTimeMs = imageRequestStartTimeMs;
  }

  public void setImageRequestEndTimeMs(long imageRequestEndTimeMs) {
    mImageRequestEndTimeMs = imageRequestEndTimeMs;
  }

  public void setImageOrigin(@ImageOrigin int imageOrigin) {
    mImageOrigin = imageOrigin;
  }

  public void setCanceled(boolean canceled) {
    mIsCanceled = canceled;
  }

  public void setSuccessful(boolean successful) {
    mIsSuccessful = successful;
  }

  public void setPrefetch(boolean prefetch) {
    mIsPrefetch = prefetch;
  }

  public void setImageInfo(@Nullable ImageInfo imageInfo) {
    mImageInfo = imageInfo;
  }

  public ImagePerfData snapshot() {
    return new ImagePerfData(
        mRequestId,
        mImageRequest,
        mCallerContext,
        mImageInfo,
        mControllerSubmitTimeMs,
        mControllerFinalImageSetTimeMs,
        mControllerFailureTimeMs,
        mImageRequestStartTimeMs,
        mImageRequestEndTimeMs,
        mImageOrigin,
        mIsCanceled,
        mIsSuccessful,
        mIsPrefetch);
  }
}

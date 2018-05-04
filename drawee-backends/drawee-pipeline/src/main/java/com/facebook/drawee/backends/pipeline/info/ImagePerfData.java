/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.drawee.backends.pipeline.info;

import com.facebook.common.internal.Objects;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.request.ImageRequest;
import javax.annotation.Nullable;

public class ImagePerfData {

  public static final int UNSET = -1;

  private final @Nullable String mRequestId;
  private final @Nullable Object mCallerContext;
  private final @Nullable ImageRequest mImageRequest;
  private final @Nullable ImageInfo mImageInfo;

  private final long mControllerSubmitTimeMs;
  private final long mControllerFinalImageSetTimeMs;
  private final long mControllerFailureTimeMs;

  private final long mImageRequestStartTimeMs;
  private final long mImageRequestEndTimeMs;
  private final @ImageOrigin int mImageOrigin;
  private final boolean mIsCanceled;
  private final boolean mIsSuccessful;
  private final boolean mIsPrefetch;

  public ImagePerfData(
      @Nullable String requestId,
      @Nullable ImageRequest imageRequest,
      @Nullable Object callerContext,
      @Nullable ImageInfo imageInfo,
      long controllerSubmitTimeMs,
      long controllerFinalImageSetTimeMs,
      long controllerFailureTimeMs,
      long imageRequestStartTimeMs,
      long imageRequestEndTimeMs,
      @ImageOrigin int imageOrigin,
      boolean isCanceled,
      boolean isSuccessful,
      boolean isPrefetch) {
    mRequestId = requestId;
    mImageRequest = imageRequest;
    mCallerContext = callerContext;
    mImageInfo = imageInfo;
    mControllerSubmitTimeMs = controllerSubmitTimeMs;
    mControllerFinalImageSetTimeMs = controllerFinalImageSetTimeMs;
    mControllerFailureTimeMs = controllerFailureTimeMs;
    mImageRequestStartTimeMs = imageRequestStartTimeMs;
    mImageRequestEndTimeMs = imageRequestEndTimeMs;
    mImageOrigin = imageOrigin;
    mIsCanceled = isCanceled;
    mIsSuccessful = isSuccessful;
    mIsPrefetch = isPrefetch;
  }

  @Nullable
  public String getRequestId() {
    return mRequestId;
  }

  @Nullable
  public ImageRequest getImageRequest() {
    return mImageRequest;
  }

  @Nullable
  public Object getCallerContext() {
    return mCallerContext;
  }

  @Nullable
  public ImageInfo getImageInfo() {
    return mImageInfo;
  }

  public long getControllerSubmitTimeMs() {
    return mControllerSubmitTimeMs;
  }

  public long getControllerFinalImageSetTimeMs() {
    return mControllerFinalImageSetTimeMs;
  }

  public long getControllerFailureTimeMs() {
    return mControllerFailureTimeMs;
  }

  public long getImageRequestStartTimeMs() {
    return mImageRequestStartTimeMs;
  }

  public long getImageRequestEndTimeMs() {
    return mImageRequestEndTimeMs;
  }

  public @ImageOrigin int getImageOrigin() {
    return mImageOrigin;
  }

  public boolean isCanceled() {
    return mIsCanceled;
  }

  public boolean isSuccessful() {
    return mIsSuccessful;
  }

  public boolean isPrefetch() {
    return mIsPrefetch;
  }

  public long getFinalImageLoadTimeMs() {
    if (isSuccessful()) {
      return getImageRequestEndTimeMs() - getImageRequestStartTimeMs();
    }
    return UNSET;
  }

  public String createDebugString() {
    return Objects.toStringHelper(this)
        .add("request ID", mRequestId)
        .add("controller submit", mControllerSubmitTimeMs)
        .add("controller final image", mControllerFinalImageSetTimeMs)
        .add("controller failure", mControllerFailureTimeMs)
        .add("start time", mImageRequestStartTimeMs)
        .add("end time", mImageRequestEndTimeMs)
        .add("origin", ImageOriginUtils.toString(mImageOrigin))
        .add("canceled", mIsCanceled)
        .add("successful", mIsSuccessful)
        .add("prefetch", mIsPrefetch)
        .add("caller context", mCallerContext)
        .add("image request", mImageRequest)
        .add("image info", mImageInfo)
        .toString();
  }
}

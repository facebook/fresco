/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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
  private @Nullable String mControllerId;
  private @Nullable String mRequestId;
  private @Nullable ImageRequest mImageRequest;
  private @Nullable Object mCallerContext;
  private @Nullable ImageInfo mImageInfo;

  // Controller timings
  private long mControllerSubmitTimeMs = UNSET;
  private long mControllerIntermediateImageSetTimeMs = UNSET;
  private long mControllerFinalImageSetTimeMs = UNSET;
  private long mControllerFailureTimeMs = UNSET;
  private long mControllerCancelTimeMs = UNSET;

  // Image request timings
  private long mImageRequestStartTimeMs = UNSET;
  private long mImageRequestEndTimeMs = UNSET;

  // Image pipeline information
  private @ImageOrigin int mImageOrigin = UNSET;
  private @Nullable String mUltimateProducerName;
  private boolean mIsPrefetch;

  // On screen information
  private int mOnScreenWidthPx = UNSET;
  private int mOnScreenHeightPx = UNSET;

  // Internal parameters
  private @ImageLoadStatus int mImageLoadStatus = ImageLoadStatus.UNKNOWN;
  // Visibility
  private @VisibilityState int mVisibilityState = VisibilityState.UNKNOWN;
  private long mVisibilityEventTimeMs = UNSET;
  private long mInvisibilityEventTimeMs = UNSET;

  private @Nullable String mComponentTag;

  public void reset() {
    mRequestId = null;
    mImageRequest = null;
    mCallerContext = null;
    mImageInfo = null;

    mControllerSubmitTimeMs = UNSET;
    mControllerFinalImageSetTimeMs = UNSET;
    mControllerFailureTimeMs = UNSET;
    mControllerCancelTimeMs = UNSET;

    mImageRequestStartTimeMs = UNSET;
    mImageRequestEndTimeMs = UNSET;

    mImageOrigin = ImageOrigin.UNKNOWN;
    mUltimateProducerName = null;
    mIsPrefetch = false;

    mOnScreenWidthPx = UNSET;
    mOnScreenHeightPx = UNSET;

    mImageLoadStatus = ImageLoadStatus.UNKNOWN;

    mVisibilityState = VisibilityState.UNKNOWN;
    mVisibilityEventTimeMs = UNSET;
    mInvisibilityEventTimeMs = UNSET;

    mComponentTag = null;
  }

  public void setImageLoadStatus(@ImageLoadStatus int imageLoadStatus) {
    mImageLoadStatus = imageLoadStatus;
  }

  @ImageLoadStatus
  public int getImageLoadStatus() {
    return mImageLoadStatus;
  }

  public void setControllerId(@Nullable String controllerId) {
    mControllerId = controllerId;
  }

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

  public void setControllerIntermediateImageSetTimeMs(long controllerIntermediateImageSetTimeMs) {
    mControllerIntermediateImageSetTimeMs = controllerIntermediateImageSetTimeMs;
  }

  public void setControllerFinalImageSetTimeMs(long controllerFinalImageSetTimeMs) {
    mControllerFinalImageSetTimeMs = controllerFinalImageSetTimeMs;
  }

  public void setControllerFailureTimeMs(long controllerFailureTimeMs) {
    mControllerFailureTimeMs = controllerFailureTimeMs;
  }

  public void setControllerCancelTimeMs(long controllerCancelTimeMs) {
    mControllerCancelTimeMs = controllerCancelTimeMs;
  }

  public void setImageRequestStartTimeMs(long imageRequestStartTimeMs) {
    mImageRequestStartTimeMs = imageRequestStartTimeMs;
  }

  public void setImageRequestEndTimeMs(long imageRequestEndTimeMs) {
    mImageRequestEndTimeMs = imageRequestEndTimeMs;
  }

  public void setVisibilityEventTimeMs(long visibilityEventTimeMs) {
    mVisibilityEventTimeMs = visibilityEventTimeMs;
  }

  public void setInvisibilityEventTimeMs(long invisibilityEventTimeMs) {
    mInvisibilityEventTimeMs = invisibilityEventTimeMs;
  }

  public void setImageOrigin(@ImageOrigin int imageOrigin) {
    mImageOrigin = imageOrigin;
  }

  public void setUltimateProducerName(@Nullable String ultimateProducerName) {
    mUltimateProducerName = ultimateProducerName;
  }

  public void setPrefetch(boolean prefetch) {
    mIsPrefetch = prefetch;
  }

  public void setImageInfo(@Nullable ImageInfo imageInfo) {
    mImageInfo = imageInfo;
  }

  public void setOnScreenWidth(int onScreenWidthPx) {
    mOnScreenWidthPx = onScreenWidthPx;
  }

  public void setOnScreenHeight(int onScreenHeightPx) {
    mOnScreenHeightPx = onScreenHeightPx;
  }

  public void setVisible(boolean visible) {
    mVisibilityState = visible ? VisibilityState.VISIBLE : VisibilityState.INVISIBLE;
  }

  public void setComponentTag(@Nullable String componentTag) {
    mComponentTag = componentTag;
  }

  public ImagePerfData snapshot() {
    return new ImagePerfData(
        mControllerId,
        mRequestId,
        mImageRequest,
        mCallerContext,
        mImageInfo,
        mControllerSubmitTimeMs,
        mControllerIntermediateImageSetTimeMs,
        mControllerFinalImageSetTimeMs,
        mControllerFailureTimeMs,
        mControllerCancelTimeMs,
        mImageRequestStartTimeMs,
        mImageRequestEndTimeMs,
        mImageOrigin,
        mUltimateProducerName,
        mIsPrefetch,
        mOnScreenWidthPx,
        mOnScreenHeightPx,
        mVisibilityState,
        mVisibilityEventTimeMs,
        mInvisibilityEventTimeMs,
        mComponentTag);
  }
}

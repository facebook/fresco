/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.backends.pipeline.info;

import static com.facebook.drawee.backends.pipeline.info.ImagePerfData.UNSET;

import com.facebook.fresco.ui.common.ControllerListener2.Extras;
import com.facebook.fresco.ui.common.DimensionsInfo;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.STRICT)
public class ImagePerfState {

  // General image metadata
  private @Nullable String mControllerId;
  private @Nullable String mRequestId;
  private @Nullable ImageRequest mImageRequest;
  private @Nullable Object mCallerContext;
  private @Nullable ImageInfo mImageInfo;

  // Controller image metadata
  private @Nullable ImageRequest mControllerImageRequest;
  private @Nullable ImageRequest mControllerLowResImageRequest;
  private @Nullable ImageRequest[] mControllerFirstAvailableImageRequests;

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
  private @ImageOrigin int mImageOrigin = ImageOrigin.UNKNOWN;
  private @Nullable String mUltimateProducerName;
  private boolean mIsPrefetch;

  // On screen information
  private int mOnScreenWidthPx = UNSET;
  private int mOnScreenHeightPx = UNSET;

  // Error data
  private @Nullable Throwable mErrorThrowable;

  // Internal parameters
  private @ImageLoadStatus int mImageLoadStatus = ImageLoadStatus.UNKNOWN;
  // Visibility
  private @VisibilityState int mVisibilityState = VisibilityState.UNKNOWN;
  private long mVisibilityEventTimeMs = UNSET;
  private long mInvisibilityEventTimeMs = UNSET;

  private long mImageDrawTimeMs = UNSET;

  private @Nullable String mComponentTag;

  private @Nullable DimensionsInfo mDimensionsInfo;

  private @Nullable Extras mExtraData;

  public void reset() {
    mRequestId = null;
    mImageRequest = null;
    mCallerContext = null;
    mImageInfo = null;

    mControllerImageRequest = null;
    mControllerLowResImageRequest = null;
    mControllerFirstAvailableImageRequests = null;

    mImageOrigin = ImageOrigin.UNKNOWN;
    mUltimateProducerName = null;
    mIsPrefetch = false;

    mOnScreenWidthPx = UNSET;
    mOnScreenHeightPx = UNSET;

    mErrorThrowable = null;

    mImageLoadStatus = ImageLoadStatus.UNKNOWN;

    mVisibilityState = VisibilityState.UNKNOWN;

    mComponentTag = null;

    mDimensionsInfo = null;

    mExtraData = null;

    resetPointsTimestamps();
  }

  /** Useful when reusing the same {@link ImagePerfState} when component is being remounted */
  public void resetPointsTimestamps() {
    mImageRequestStartTimeMs = UNSET;
    mImageRequestEndTimeMs = UNSET;

    mControllerSubmitTimeMs = UNSET;
    mControllerFinalImageSetTimeMs = UNSET;
    mControllerFailureTimeMs = UNSET;
    mControllerCancelTimeMs = UNSET;

    mVisibilityEventTimeMs = UNSET;
    mInvisibilityEventTimeMs = UNSET;

    mImageDrawTimeMs = UNSET;
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

  public void setControllerImageRequests(
      @Nullable ImageRequest imageRequest,
      @Nullable ImageRequest lowResImageRequest,
      @Nullable ImageRequest[] firstAvailableImageRequests) {
    mControllerImageRequest = imageRequest;
    mControllerLowResImageRequest = lowResImageRequest;
    mControllerFirstAvailableImageRequests = firstAvailableImageRequests;
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

  public void setErrorThrowable(@Nullable Throwable errorThrowable) {
    mErrorThrowable = errorThrowable;
  }

  public void setVisible(boolean visible) {
    mVisibilityState = visible ? VisibilityState.VISIBLE : VisibilityState.INVISIBLE;
  }

  public void setComponentTag(@Nullable String componentTag) {
    mComponentTag = componentTag;
  }

  public void setImageDrawTimeMs(long imageDrawTimeMs) {
    mImageDrawTimeMs = imageDrawTimeMs;
  }

  public ImagePerfData snapshot() {
    return new ImagePerfData(
        mControllerId,
        mRequestId,
        mImageRequest,
        mCallerContext,
        mImageInfo,
        mControllerImageRequest,
        mControllerLowResImageRequest,
        mControllerFirstAvailableImageRequests,
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
        mErrorThrowable,
        mVisibilityState,
        mVisibilityEventTimeMs,
        mInvisibilityEventTimeMs,
        mComponentTag,
        mImageDrawTimeMs,
        mDimensionsInfo,
        mExtraData);
  }

  public long getImageDrawTimeMs() {
    return mImageDrawTimeMs;
  }

  public void setDimensionsInfo(DimensionsInfo dimensionsInfo) {
    mDimensionsInfo = dimensionsInfo;
  }

  public @Nullable DimensionsInfo getDimensionsInfo() {
    return mDimensionsInfo;
  }

  public void setExtraData(@Nullable Extras extraData) {
    mExtraData = extraData;
  }

  public @Nullable Object getExtraData() {
    return mExtraData;
  }
}

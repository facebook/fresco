/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.ui.common;

import static com.facebook.fresco.ui.common.ImagePerfData.UNSET;

import com.facebook.fresco.ui.common.ControllerListener2.Extras;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class ImagePerfState {

  // General image metadata
  private @Nullable String mControllerId;
  private @Nullable String mRequestId;
  private @Nullable Object mImageRequest;
  private @Nullable Object mCallerContext;
  private @Nullable Object mImageInfo;

  // Controller timings
  private long mControllerSubmitTimeMs = UNSET;
  private long mControllerIntermediateImageSetTimeMs = UNSET;
  private long mControllerFinalImageSetTimeMs = UNSET;
  private long mControllerFailureTimeMs = UNSET;

  // Image request timings
  private long mImageRequestStartTimeMs = UNSET;
  private long mImageRequestEndTimeMs = UNSET;

  // Image pipeline information
  private boolean mIsPrefetch;

  // On screen information
  private int mOnScreenWidthPx = UNSET;
  private int mOnScreenHeightPx = UNSET;

  // Error data
  private @Nullable Throwable mErrorThrowable;

  // Internal parameters
  private ImageLoadStatus mImageLoadStatus = ImageLoadStatus.UNKNOWN;
  // Visibility
  private VisibilityState mVisibilityState = VisibilityState.UNKNOWN;
  private long mVisibilityEventTimeMs = UNSET;
  private long mInvisibilityEventTimeMs = UNSET;
  // Fetch efficiency
  private @Nullable DimensionsInfo mDimensionsInfo;
  // Pipeline and view extras
  private @Nullable Extras mExtraData;

  public void reset() {
    mRequestId = null;
    mImageRequest = null;
    mCallerContext = null;
    mImageInfo = null;

    mIsPrefetch = false;

    mOnScreenWidthPx = UNSET;
    mOnScreenHeightPx = UNSET;

    mErrorThrowable = null;

    mImageLoadStatus = ImageLoadStatus.UNKNOWN;

    mVisibilityState = VisibilityState.UNKNOWN;

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

    mVisibilityEventTimeMs = UNSET;
    mInvisibilityEventTimeMs = UNSET;
  }

  public void setImageLoadStatus(ImageLoadStatus imageLoadStatus) {
    mImageLoadStatus = imageLoadStatus;
  }

  public ImageLoadStatus getImageLoadStatus() {
    return mImageLoadStatus;
  }

  public void setControllerId(@Nullable String controllerId) {
    mControllerId = controllerId;
  }

  public void setRequestId(@Nullable String requestId) {
    mRequestId = requestId;
  }

  public void setImageRequest(@Nullable Object imageRequest) {
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

  public void setPrefetch(boolean prefetch) {
    mIsPrefetch = prefetch;
  }

  public void setImageInfo(@Nullable Object imageInfo) {
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
        mImageRequestStartTimeMs,
        mImageRequestEndTimeMs,
        mIsPrefetch,
        mOnScreenWidthPx,
        mOnScreenHeightPx,
        mErrorThrowable,
        mVisibilityState,
        mVisibilityEventTimeMs,
        mInvisibilityEventTimeMs,
        mDimensionsInfo,
        mExtraData);
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

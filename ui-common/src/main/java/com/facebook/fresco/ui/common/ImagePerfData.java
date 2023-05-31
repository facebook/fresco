/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.ui.common;

import com.facebook.common.internal.Objects;
import com.facebook.fresco.ui.common.ControllerListener2.Extras;
import com.facebook.infer.annotation.Nullsafe;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class ImagePerfData {

  public static final int UNSET = -1;

  private final @Nullable String mControllerId;
  private final @Nullable String mRequestId;
  private final @Nullable Object mCallerContext;
  private final @Nullable Object mImageRequest;
  private final @Nullable Object mImageInfo;

  private final long mControllerSubmitTimeMs;
  private final long mControllerIntermediateImageSetTimeMs;
  private final long mControllerFinalImageSetTimeMs;
  private final long mControllerFailureTimeMs;
  private final long mControllerCancelTimeMs;

  private final long mImageRequestStartTimeMs;
  private final long mImageRequestEndTimeMs;
  private final boolean mIsPrefetch;

  private final int mOnScreenWidthPx;
  private final int mOnScreenHeightPx;

  private final @Nullable Throwable mErrorThrowable;

  // Visibility
  private final VisibilityState mVisibilityState;
  private final long mVisibilityEventTimeMs;
  private final long mInvisibilityEventTimeMs;

  private final long mImageDrawTimeMs;

  private final @Nullable DimensionsInfo mDimensionsInfo;

  private @Nullable Extras mExtraData;

  private Map<String, Object> mPipelineExtras;

  public ImagePerfData(
      @Nullable String controllerId,
      @Nullable String requestId,
      @Nullable Object imageRequest,
      @Nullable Object callerContext,
      @Nullable Object imageInfo,
      long controllerSubmitTimeMs,
      long controllerIntermediateImageSetTimeMs,
      long controllerFinalImageSetTimeMs,
      long controllerFailureTimeMs,
      long controllerCancelTimeMs,
      long imageRequestStartTimeMs,
      long imageRequestEndTimeMs,
      boolean isPrefetch,
      int onScreenWidthPx,
      int onScreenHeightPx,
      @Nullable Throwable errorThrowable,
      VisibilityState visibilityState,
      long visibilityEventTimeMs,
      long invisibilityEventTime,
      long imageDrawTimeMs,
      @Nullable DimensionsInfo dimensionsInfo,
      @Nullable Extras extraData,
      Map<String, Object> pipelineExtras) {
    mControllerId = controllerId;
    mRequestId = requestId;
    mImageRequest = imageRequest;
    mCallerContext = callerContext;
    mImageInfo = imageInfo;
    mControllerSubmitTimeMs = controllerSubmitTimeMs;
    mControllerIntermediateImageSetTimeMs = controllerIntermediateImageSetTimeMs;
    mControllerFinalImageSetTimeMs = controllerFinalImageSetTimeMs;
    mControllerFailureTimeMs = controllerFailureTimeMs;
    mControllerCancelTimeMs = controllerCancelTimeMs;
    mImageRequestStartTimeMs = imageRequestStartTimeMs;
    mImageRequestEndTimeMs = imageRequestEndTimeMs;
    mIsPrefetch = isPrefetch;
    mOnScreenWidthPx = onScreenWidthPx;
    mOnScreenHeightPx = onScreenHeightPx;
    mErrorThrowable = errorThrowable;
    mVisibilityState = visibilityState;
    mVisibilityEventTimeMs = visibilityEventTimeMs;
    mInvisibilityEventTimeMs = invisibilityEventTime;
    mImageDrawTimeMs = imageDrawTimeMs;
    mDimensionsInfo = dimensionsInfo;
    mExtraData = extraData;
    mPipelineExtras = new HashMap<>(pipelineExtras);
  }

  public long getImageDrawTimeMs() {
    return mImageDrawTimeMs;
  }

  @Nullable
  public String getControllerId() {
    return mControllerId;
  }

  @Nullable
  public String getRequestId() {
    return mRequestId;
  }

  @Nullable
  public Object getImageRequest() {
    return mImageRequest;
  }

  @Nullable
  public Object getCallerContext() {
    return mCallerContext;
  }

  @Nullable
  public Object getImageInfo() {
    return mImageInfo;
  }

  public long getControllerSubmitTimeMs() {
    return mControllerSubmitTimeMs;
  }

  public long getControllerIntermediateImageSetTimeMs() {
    return mControllerIntermediateImageSetTimeMs;
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

  public boolean isPrefetch() {
    return mIsPrefetch;
  }

  public int getOnScreenWidthPx() {
    return mOnScreenWidthPx;
  }

  public int getOnScreenHeightPx() {
    return mOnScreenHeightPx;
  }

  @Nullable
  public Throwable getErrorThrowable() {
    return mErrorThrowable;
  }

  public long getFinalImageLoadTimeMs() {
    if (getImageRequestEndTimeMs() == UNSET || getImageRequestStartTimeMs() == UNSET) {
      return UNSET;
    }

    return getImageRequestEndTimeMs() - getImageRequestStartTimeMs();
  }

  public long getIntermediateImageLoadTimeMs() {
    return mControllerIntermediateImageSetTimeMs;
  }

  public VisibilityState getVisibilityState() {
    return mVisibilityState;
  }

  public long getVisibilityEventTimeMs() {
    return mVisibilityEventTimeMs;
  }

  public long getInvisibilityEventTimeMs() {
    return mInvisibilityEventTimeMs;
  }

  @Nullable
  public DimensionsInfo getDimensionsInfo() {
    return mDimensionsInfo;
  }

  @Nullable
  public Extras getExtraData() {
    return mExtraData;
  }

  public void setExtraData(Extras extraData) {
    mExtraData = extraData;
  }

  @Nullable
  public Object getPipelineExtra(String key) {
    if (mPipelineExtras.containsKey(key)) {
      return mPipelineExtras.get(key);
    } else {
      return null;
    }
  }

  public String createDebugString() {
    return Objects.toStringHelper(this)
        .add("controller ID", mControllerId)
        .add("request ID", mRequestId)
        .add("controller submit", mControllerSubmitTimeMs)
        .add("controller final image", mControllerFinalImageSetTimeMs)
        .add("controller failure", mControllerFailureTimeMs)
        .add("controller cancel", mControllerCancelTimeMs)
        .add("start time", mImageRequestStartTimeMs)
        .add("end time", mImageRequestEndTimeMs)
        .add("prefetch", mIsPrefetch)
        .add("caller context", mCallerContext)
        .add("image request", mImageRequest)
        .add("image info", mImageInfo)
        .add("on-screen width", mOnScreenWidthPx)
        .add("on-screen height", mOnScreenHeightPx)
        .add("visibility state", mVisibilityState)
        .add("visibility event", mVisibilityEventTimeMs)
        .add("invisibility event", mInvisibilityEventTimeMs)
        .add("image draw event", mImageDrawTimeMs)
        .add("dimensions info", mDimensionsInfo)
        .add("extra data", mExtraData)
        .add("pipeline extras", mPipelineExtras)
        .toString();
  }
}

/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.backends.pipeline.info;

import com.facebook.common.internal.Objects;
import com.facebook.fresco.ui.common.ControllerListener2.Extras;
import com.facebook.fresco.ui.common.DimensionsInfo;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.STRICT)
public class ImagePerfData {

  public static final int UNSET = -1;

  private final @Nullable String mControllerId;
  private final @Nullable String mRequestId;
  private final @Nullable Object mCallerContext;
  private final @Nullable ImageRequest mImageRequest;
  private final @Nullable ImageInfo mImageInfo;

  // Controller image metadata
  private final @Nullable ImageRequest mControllerImageRequest;
  private final @Nullable ImageRequest mControllerLowResImageRequest;
  private final @Nullable ImageRequest[] mControllerFirstAvailableImageRequests;

  private final long mControllerSubmitTimeMs;
  private final long mControllerIntermediateImageSetTimeMs;
  private final long mControllerFinalImageSetTimeMs;
  private final long mControllerFailureTimeMs;
  private final long mControllerCancelTimeMs;

  private final long mImageRequestStartTimeMs;
  private final long mImageRequestEndTimeMs;
  private final @ImageOrigin int mImageOrigin;
  private final @Nullable String mUltimateProducerName;
  private final boolean mIsPrefetch;

  private final int mOnScreenWidthPx;
  private final int mOnScreenHeightPx;

  private final @Nullable Throwable mErrorThrowable;

  // Visibility
  @VisibilityState private final int mVisibilityState;
  private final long mVisibilityEventTimeMs;
  private final long mInvisibilityEventTimeMs;

  private final @Nullable String mComponentTag;

  private final long mImageDrawTimeMs;

  private final @Nullable DimensionsInfo mDimensionsInfo;

  private @Nullable Extras mExtraData;

  public ImagePerfData(
      @Nullable String controllerId,
      @Nullable String requestId,
      @Nullable ImageRequest imageRequest,
      @Nullable Object callerContext,
      @Nullable ImageInfo imageInfo,
      @Nullable ImageRequest controllerImageRequest,
      @Nullable ImageRequest controllerLowResImageRequest,
      @Nullable ImageRequest[] controllerFirstAvailableImageRequests,
      long controllerSubmitTimeMs,
      long controllerIntermediateImageSetTimeMs,
      long controllerFinalImageSetTimeMs,
      long controllerFailureTimeMs,
      long controllerCancelTimeMs,
      long imageRequestStartTimeMs,
      long imageRequestEndTimeMs,
      @ImageOrigin int imageOrigin,
      @Nullable String ultimateProducerName,
      boolean isPrefetch,
      int onScreenWidthPx,
      int onScreenHeightPx,
      @Nullable Throwable errorThrowable,
      int visibilityState,
      long visibilityEventTimeMs,
      long invisibilityEventTime,
      @Nullable String componentTag,
      long imageDrawTimeMs,
      @Nullable DimensionsInfo dimensionsInfo,
      @Nullable Extras extraData) {
    mControllerId = controllerId;
    mRequestId = requestId;
    mImageRequest = imageRequest;
    mCallerContext = callerContext;
    mImageInfo = imageInfo;
    mControllerImageRequest = controllerImageRequest;
    mControllerLowResImageRequest = controllerLowResImageRequest;
    mControllerFirstAvailableImageRequests = controllerFirstAvailableImageRequests;
    mControllerSubmitTimeMs = controllerSubmitTimeMs;
    mControllerIntermediateImageSetTimeMs = controllerIntermediateImageSetTimeMs;
    mControllerFinalImageSetTimeMs = controllerFinalImageSetTimeMs;
    mControllerFailureTimeMs = controllerFailureTimeMs;
    mControllerCancelTimeMs = controllerCancelTimeMs;
    mImageRequestStartTimeMs = imageRequestStartTimeMs;
    mImageRequestEndTimeMs = imageRequestEndTimeMs;
    mImageOrigin = imageOrigin;
    mUltimateProducerName = ultimateProducerName;
    mIsPrefetch = isPrefetch;
    mOnScreenWidthPx = onScreenWidthPx;
    mOnScreenHeightPx = onScreenHeightPx;
    mErrorThrowable = errorThrowable;
    mVisibilityState = visibilityState;
    mVisibilityEventTimeMs = visibilityEventTimeMs;
    mInvisibilityEventTimeMs = invisibilityEventTime;
    mComponentTag = componentTag;
    mImageDrawTimeMs = imageDrawTimeMs;
    mDimensionsInfo = dimensionsInfo;
    mExtraData = extraData;
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

  public long getControllerIntermediateImageSetTimeMs() {
    return mControllerIntermediateImageSetTimeMs;
  }

  public long getControllerFinalImageSetTimeMs() {
    return mControllerFinalImageSetTimeMs;
  }

  public long getControllerFailureTimeMs() {
    return mControllerFailureTimeMs;
  }

  @Nullable
  public ImageRequest getControllerImageRequest() {
    return mControllerImageRequest;
  }

  @Nullable
  public ImageRequest getControllerLowResImageRequest() {
    return mControllerLowResImageRequest;
  }

  @Nullable
  public ImageRequest[] getControllerFirstAvailableImageRequests() {
    return mControllerFirstAvailableImageRequests;
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

  @Nullable
  public String getUltimateProducerName() {
    return mUltimateProducerName;
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
    if (getControllerIntermediateImageSetTimeMs() == UNSET
        || getControllerSubmitTimeMs() == UNSET) {
      return UNSET;
    }

    return getControllerIntermediateImageSetTimeMs() - getControllerSubmitTimeMs();
  }

  public int getVisibilityState() {
    return mVisibilityState;
  }

  public long getVisibilityEventTimeMs() {
    return mVisibilityEventTimeMs;
  }

  public long getInvisibilityEventTimeMs() {
    return mInvisibilityEventTimeMs;
  }

  @Nullable
  public String getComponentTag() {
    return mComponentTag;
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

  public String createDebugString() {
    return Objects.toStringHelper(this)
        .add("controller ID", mControllerId)
        .add("request ID", mRequestId)
        .add("controller image request", mControllerImageRequest)
        .add("controller low res image request", mControllerLowResImageRequest)
        .add("controller first available image requests", mControllerFirstAvailableImageRequests)
        .add("controller submit", mControllerSubmitTimeMs)
        .add("controller final image", mControllerFinalImageSetTimeMs)
        .add("controller failure", mControllerFailureTimeMs)
        .add("controller cancel", mControllerCancelTimeMs)
        .add("start time", mImageRequestStartTimeMs)
        .add("end time", mImageRequestEndTimeMs)
        .add("origin", ImageOriginUtils.toString(mImageOrigin))
        .add("ultimateProducerName", mUltimateProducerName)
        .add("prefetch", mIsPrefetch)
        .add("caller context", mCallerContext)
        .add("image request", mImageRequest)
        .add("image info", mImageInfo)
        .add("on-screen width", mOnScreenWidthPx)
        .add("on-screen height", mOnScreenHeightPx)
        .add("visibility state", mVisibilityState)
        .add("component tag", mComponentTag)
        .add("visibility event", mVisibilityEventTimeMs)
        .add("invisibility event", mInvisibilityEventTimeMs)
        .add("image draw event", mImageDrawTimeMs)
        .add("dimensions info", mDimensionsInfo)
        .add("extra data", mExtraData)
        .toString();
  }
}

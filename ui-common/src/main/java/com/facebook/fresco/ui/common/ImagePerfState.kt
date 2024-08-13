/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.ui.common

import com.facebook.fresco.ui.common.ControllerListener2.Extras

class ImagePerfState(infra: ImageRenderingInfra) : ImagePerfLoggingState(infra) {

  // General image metadata
  private var controllerId: String? = null
  private var requestId: String? = null
  private var imageRequest: Any? = null
  var callerContext: Any? = null
  private var imageInfo: Any? = null
  // Controller timings
  private var controllerSubmitTimeMs: Long = ImagePerfData.UNSET.toLong()
  private var controllerIntermediateImageSetTimeMs: Long = ImagePerfData.UNSET.toLong()
  private var controllerFinalImageSetTimeMs: Long = ImagePerfData.UNSET.toLong()
  private var controllerFailureTimeMs: Long = ImagePerfData.UNSET.toLong()
  // Image request timings
  private var imageRequestStartTimeMs: Long = ImagePerfData.UNSET.toLong()
  private var imageRequestEndTimeMs: Long = ImagePerfData.UNSET.toLong()
  // Image pipeline information
  private var isPrefetch = false
  // On screen information
  private var onScreenWidthPx: Int = ImagePerfData.UNSET
  private var onScreenHeightPx: Int = ImagePerfData.UNSET
  // Error data
  private var errorThrowable: Throwable? = null
  // Internal parameters
  var imageLoadStatus: ImageLoadStatus = ImageLoadStatus.UNKNOWN

  // Visibility
  private var visibilityState = VisibilityState.UNKNOWN
  private var visibilityEventTimeMs: Long = ImagePerfData.UNSET.toLong()
  private var invisibilityEventTimeMs: Long = ImagePerfData.UNSET.toLong()
  // Fetch efficiency
  var dimensionsInfo: DimensionsInfo? = null

  // Pipeline and view extras
  private var _extraData: Extras? = null

  fun reset() {
    requestId = null
    imageRequest = null
    callerContext = null
    imageInfo = null
    isPrefetch = false
    onScreenWidthPx = ImagePerfData.UNSET
    onScreenHeightPx = ImagePerfData.UNSET
    errorThrowable = null
    imageLoadStatus = ImageLoadStatus.UNKNOWN
    visibilityState = VisibilityState.UNKNOWN
    dimensionsInfo = null
    _extraData = null
    resetPointsTimestamps()

    resetLoggingState()
  }

  /** Useful when reusing the same [ImagePerfState] when component is being remounted */
  fun resetPointsTimestamps() {
    imageRequestStartTimeMs = ImagePerfData.UNSET.toLong()
    imageRequestEndTimeMs = ImagePerfData.UNSET.toLong()
    controllerSubmitTimeMs = ImagePerfData.UNSET.toLong()
    controllerFinalImageSetTimeMs = ImagePerfData.UNSET.toLong()
    controllerFailureTimeMs = ImagePerfData.UNSET.toLong()
    visibilityEventTimeMs = ImagePerfData.UNSET.toLong()
    invisibilityEventTimeMs = ImagePerfData.UNSET.toLong()

    // Are these really required here? Adding to be safe for now, but verify its utility later.
    newIntermediateImageSetPointAvailable = false
    intermediateImageSetTimes.clear()
  }

  fun setControllerId(controllerId: String?) {
    this.controllerId = controllerId
  }

  fun setRequestId(requestId: String?) {
    this.requestId = requestId
  }

  fun setImageRequest(imageRequest: Any?) {
    this.imageRequest = imageRequest
  }

  fun setControllerSubmitTimeMs(controllerSubmitTimeMs: Long) {
    this.controllerSubmitTimeMs = controllerSubmitTimeMs
  }

  fun setControllerIntermediateImageSetTimeMs(controllerIntermediateImageSetTimeMs: Long) {
    this.controllerIntermediateImageSetTimeMs = controllerIntermediateImageSetTimeMs
  }

  fun setControllerFinalImageSetTimeMs(controllerFinalImageSetTimeMs: Long) {
    this.controllerFinalImageSetTimeMs = controllerFinalImageSetTimeMs
  }

  fun setControllerFailureTimeMs(controllerFailureTimeMs: Long) {
    this.controllerFailureTimeMs = controllerFailureTimeMs
  }

  fun setImageRequestStartTimeMs(imageRequestStartTimeMs: Long) {
    this.imageRequestStartTimeMs = imageRequestStartTimeMs
  }

  fun setImageRequestEndTimeMs(imageRequestEndTimeMs: Long) {
    this.imageRequestEndTimeMs = imageRequestEndTimeMs
  }

  fun setVisibilityEventTimeMs(visibilityEventTimeMs: Long) {
    this.visibilityEventTimeMs = visibilityEventTimeMs
  }

  fun setInvisibilityEventTimeMs(invisibilityEventTimeMs: Long) {
    this.invisibilityEventTimeMs = invisibilityEventTimeMs
  }

  fun setPrefetch(prefetch: Boolean) {
    isPrefetch = prefetch
  }

  fun setImageInfo(imageInfo: Any?) {
    this.imageInfo = imageInfo
  }

  fun setOnScreenWidth(onScreenWidthPx: Int) {
    this.onScreenWidthPx = onScreenWidthPx
  }

  fun setOnScreenHeight(onScreenHeightPx: Int) {
    this.onScreenHeightPx = onScreenHeightPx
  }

  fun setErrorThrowable(errorThrowable: Throwable?) {
    this.errorThrowable = errorThrowable
  }

  fun setVisible(visible: Boolean) {
    visibilityState = if (visible) VisibilityState.VISIBLE else VisibilityState.INVISIBLE
  }

  fun snapshot(): ImagePerfData =
      ImagePerfData(
          infra,
          controllerId,
          requestId,
          imageRequest,
          callerContext,
          imageInfo,
          controllerSubmitTimeMs,
          controllerIntermediateImageSetTimeMs,
          controllerFinalImageSetTimeMs,
          controllerFailureTimeMs,
          imageRequestStartTimeMs,
          imageRequestEndTimeMs,
          emptyEventTimestampNs,
          releasedEventTimestampNs,
          isPrefetch,
          onScreenWidthPx,
          onScreenHeightPx,
          errorThrowable,
          visibilityState,
          visibilityEventTimeMs,
          invisibilityEventTimeMs,
          dimensionsInfo,
          _extraData,
          callingClassNameOnVisible,
          rootContextNameOnVisible,
          contextChainArrayOnVisible,
          contextChainExtrasOnVisible,
          contentIdOnVisible,
          surfaceOnVisible,
          subSurfaceOnVisible,
          msSinceLastNavigationOnVisible,
          startupStatusOnVisible,
          intermediateImageSetTimes.toList(),
          newIntermediateImageSetPointAvailable,
          errorMessageOnFailure,
          errorCodeOnFailure,
          densityDpiOnSuccess,
      )

  fun setExtraData(extraData: Extras?) {
    _extraData = extraData
  }

  val extraData: Any?
    get() = _extraData
}

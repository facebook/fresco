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
  private var controllerSubmitTimeMs: Long = ImagePerfData.UNSET
  private var controllerIntermediateImageSetTimeMs: Long = ImagePerfData.UNSET
  private var controllerFinalImageSetTimeMs: Long = ImagePerfData.UNSET
  private var controllerFailureTimeMs: Long = ImagePerfData.UNSET
  // Image request timings
  private var imageRequestStartTimeMs: Long = ImagePerfData.UNSET
  private var imageRequestEndTimeMs: Long = ImagePerfData.UNSET
  // Image pipeline information
  private var isPrefetch = false
  // On screen information
  private var onScreenWidthPx: Int = ImagePerfData.UNSET.toInt()
  private var onScreenHeightPx: Int = ImagePerfData.UNSET.toInt()
  // Error data
  private var errorThrowable: Throwable? = null
  // Should be removed
  var imageLoadStatus: ImageLoadStatus = ImageLoadStatus.UNKNOWN

  // Visibility
  private var visibilityState = VisibilityState.UNKNOWN
  var visibilityEventTimeMs: Long = ImagePerfData.UNSET
  private var invisibilityEventTimeMs: Long = ImagePerfData.UNSET
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
    onScreenWidthPx = ImagePerfData.UNSET.toInt()
    onScreenHeightPx = ImagePerfData.UNSET.toInt()
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
    imageRequestStartTimeMs = ImagePerfData.UNSET
    imageRequestEndTimeMs = ImagePerfData.UNSET
    controllerSubmitTimeMs = ImagePerfData.UNSET
    controllerFinalImageSetTimeMs = ImagePerfData.UNSET
    controllerFailureTimeMs = ImagePerfData.UNSET
    visibilityEventTimeMs = ImagePerfData.UNSET
    invisibilityEventTimeMs = ImagePerfData.UNSET

    // ImagePerfLoggingState specific params
    // Are these really required here? Adding to be safe for now, but verify its utility later.
    intermediateImageSetTimes.clear()
    newIntermediateImageSetPointAvailable = false
    emptyEventTimestampNs = null
    releasedEventTimestampNs = null
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
          errorStacktraceStringOnFailure,
          errorCodeOnFailure,
          densityDpiOnSuccess,
      )

  fun setExtraData(extraData: Extras?) {
    _extraData = extraData
  }

  val extraData: Any?
    get() = _extraData
}

/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.ui.common

open class ImagePerfLoggingState(val infra: ImageRenderingInfra) {

  /** Intermediate image point names with timestamps in nanoseconds */
  val intermediateImageSetTimes: MutableList<Pair<String, Long>> = mutableListOf()

  /** Stopgap measure before we refactor the logger to store-and-replay mode */
  var newIntermediateImageSetPointAvailable: Boolean = false

  var emptyEventTimestampNs: Long? = null
  var releasedEventTimestampNs: Long? = null

  var callingClassNameOnVisible: String? = null
  var rootContextNameOnVisible: String? = null
  var contextChainArrayOnVisible: Array<String>? = null
  var contextChainExtrasOnVisible: String? = null
  var contentIdOnVisible: String? = null
  var surfaceOnVisible: String? = null
  var subSurfaceOnVisible: String? = null
  var msSinceLastNavigationOnVisible: Long? = null
  var startupStatusOnVisible: String? = null

  var errorMessageOnFailure: String? = null
  var errorStacktraceStringOnFailure: String? = null
  var errorCodeOnFailure: Int? = null

  var densityDpiOnSuccess: Int? = null

  internal fun resetLoggingState() {
    intermediateImageSetTimes.clear()
    newIntermediateImageSetPointAvailable = false

    emptyEventTimestampNs = null
    releasedEventTimestampNs = null

    callingClassNameOnVisible = null
    rootContextNameOnVisible = null
    contextChainArrayOnVisible = null
    contextChainExtrasOnVisible = null
    contentIdOnVisible = null
    surfaceOnVisible = null
    subSurfaceOnVisible = null
    msSinceLastNavigationOnVisible = null
    startupStatusOnVisible = null

    errorMessageOnFailure = null
    errorStacktraceStringOnFailure = null
    errorCodeOnFailure = null

    densityDpiOnSuccess = null
  }
}

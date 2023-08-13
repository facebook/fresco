/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.listener

import android.os.SystemClock
import android.util.Pair
import com.facebook.common.logging.FLog
import com.facebook.imagepipeline.request.ImageRequest
import java.util.HashMap
import javax.annotation.concurrent.GuardedBy

/** Logging for [ImageRequest]s. */
class RequestLoggingListener : RequestListener {

  @GuardedBy("this")
  private val producerStartTimeMap: MutableMap<Pair<String, String>, Long> = HashMap()

  @GuardedBy("this") private val requestStartTimeMap: MutableMap<String, Long> = HashMap()

  @Synchronized
  override fun onRequestStart(
      request: ImageRequest,
      callerContextObject: Any,
      requestId: String,
      isPrefetch: Boolean
  ) {
    if (FLog.isLoggable(FLog.VERBOSE)) {
      FLog.v(
          TAG,
          "time %d: onRequestSubmit: {requestId: %s, callerContext: %s, isPrefetch: %b}",
          time,
          requestId,
          callerContextObject,
          isPrefetch)
      requestStartTimeMap[requestId] = time
    }
  }

  @Synchronized
  override fun onProducerStart(requestId: String, producerName: String) {
    if (FLog.isLoggable(FLog.VERBOSE)) {
      val mapKey = Pair.create(requestId, producerName)
      val startTime = time
      producerStartTimeMap[mapKey] = startTime
      FLog.v(
          TAG,
          "time %d: onProducerStart: {requestId: %s, producer: %s}",
          startTime,
          requestId,
          producerName)
    }
  }

  @Synchronized
  override fun onProducerFinishWithSuccess(
      requestId: String,
      producerName: String,
      extraMap: Map<String, String>?
  ) {
    if (FLog.isLoggable(FLog.VERBOSE)) {
      val mapKey = Pair.create(requestId, producerName)
      val startTime = producerStartTimeMap.remove(mapKey)
      val currentTime = time
      FLog.v(
          TAG,
          "time %d: onProducerFinishWithSuccess: {requestId: %s, producer: %s, elapsedTime: %d ms, extraMap: %s}",
          currentTime,
          requestId,
          producerName,
          getElapsedTime(startTime, currentTime),
          extraMap)
    }
  }

  @Synchronized
  override fun onProducerFinishWithFailure(
      requestId: String,
      producerName: String,
      throwable: Throwable,
      extraMap: Map<String, String>?
  ) {
    if (FLog.isLoggable(FLog.WARN)) {
      val mapKey = Pair.create(requestId, producerName)
      val startTime = producerStartTimeMap.remove(mapKey)
      val currentTime = time
      FLog.w(
          TAG,
          throwable,
          "time %d: onProducerFinishWithFailure: {requestId: %s, stage: %s, elapsedTime: %d ms, extraMap: %s, throwable: %s}",
          currentTime,
          requestId,
          producerName,
          getElapsedTime(startTime, currentTime),
          extraMap,
          throwable.toString())
    }
  }

  @Synchronized
  override fun onProducerFinishWithCancellation(
      requestId: String,
      producerName: String,
      extraMap: Map<String, String>?
  ) {
    if (FLog.isLoggable(FLog.VERBOSE)) {
      val mapKey = Pair.create(requestId, producerName)
      val startTime = producerStartTimeMap.remove(mapKey)
      val currentTime = time
      FLog.v(
          TAG,
          "time %d: onProducerFinishWithCancellation: {requestId: %s, stage: %s, elapsedTime: %d ms, extraMap: %s}",
          currentTime,
          requestId,
          producerName,
          getElapsedTime(startTime, currentTime),
          extraMap)
    }
  }

  @Synchronized
  override fun onProducerEvent(requestId: String, producerName: String, producerEventName: String) {
    if (FLog.isLoggable(FLog.VERBOSE)) {
      val mapKey = Pair.create(requestId, producerName)
      val startTime = producerStartTimeMap[mapKey]
      val currentTime = time
      FLog.v(
          TAG,
          "time %d: onProducerEvent: {requestId: %s, stage: %s, eventName: %s; elapsedTime: %d ms}",
          time,
          requestId,
          producerName,
          producerEventName,
          getElapsedTime(startTime, currentTime))
    }
  }

  @Synchronized
  override fun onUltimateProducerReached(
      requestId: String,
      producerName: String,
      successful: Boolean
  ) {
    if (FLog.isLoggable(FLog.VERBOSE)) {
      val mapKey = Pair.create(requestId, producerName)
      val startTime = producerStartTimeMap.remove(mapKey)
      val currentTime = time
      FLog.v(
          TAG,
          "time %d: onUltimateProducerReached: {requestId: %s, producer: %s, elapsedTime: %d ms, success: %b}",
          currentTime,
          requestId,
          producerName,
          getElapsedTime(startTime, currentTime),
          successful)
    }
  }

  @Synchronized
  override fun onRequestSuccess(request: ImageRequest, requestId: String, isPrefetch: Boolean) {
    if (FLog.isLoggable(FLog.VERBOSE)) {
      val startTime = requestStartTimeMap.remove(requestId)
      val currentTime = time
      FLog.v(
          TAG,
          "time %d: onRequestSuccess: {requestId: %s, elapsedTime: %d ms}",
          currentTime,
          requestId,
          getElapsedTime(startTime, currentTime))
    }
  }

  @Synchronized
  override fun onRequestFailure(
      request: ImageRequest,
      requestId: String,
      throwable: Throwable,
      isPrefetch: Boolean
  ) {
    if (FLog.isLoggable(FLog.WARN)) {
      val startTime = requestStartTimeMap.remove(requestId)
      val currentTime = time
      FLog.w(
          TAG,
          "time %d: onRequestFailure: {requestId: %s, elapsedTime: %d ms, throwable: %s}",
          currentTime,
          requestId,
          getElapsedTime(startTime, currentTime),
          throwable.toString())
    }
  }

  @Synchronized
  override fun onRequestCancellation(requestId: String) {
    if (FLog.isLoggable(FLog.VERBOSE)) {
      val startTime = requestStartTimeMap.remove(requestId)
      val currentTime = time
      FLog.v(
          TAG,
          "time %d: onRequestCancellation: {requestId: %s, elapsedTime: %d ms}",
          currentTime,
          requestId,
          getElapsedTime(startTime, currentTime))
    }
  }

  override fun requiresExtraMap(id: String): Boolean = FLog.isLoggable(FLog.VERBOSE)

  companion object {
    private const val TAG = "RequestLoggingListener"

    private fun getElapsedTime(startTime: Long?, endTime: Long): Long =
        if (startTime != null) {
          endTime - startTime
        } else {
          -1
        }

    private val time: Long
      get() = SystemClock.uptimeMillis()
  }
}

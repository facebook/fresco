/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */


package com.facebook.imagepipeline.listener;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

import java.util.HashMap;
import java.util.Map;

import android.os.SystemClock;
import android.util.Pair;

import com.facebook.common.logging.FLog;
import com.facebook.imagepipeline.request.ImageRequest;

/**
 * Logging for {@link ImageRequest}s.
 */
public class RequestLoggingListener implements RequestListener {
  private static final String TAG = "RequestLoggingListener";

  @GuardedBy("this")
  private final Map<Pair<String, String>, Long> mProducerStartTimeMap;
  @GuardedBy("this")
  private final Map<String, Long> mRequestStartTimeMap;

  public RequestLoggingListener() {
    mProducerStartTimeMap = new HashMap<>();
    mRequestStartTimeMap = new HashMap<>();
  }

  @Override
  public synchronized void onRequestStart(
      ImageRequest request,
      Object callerContextObject,
      String requestId,
      boolean isPrefetch) {
    if (FLog.isLoggable(FLog.VERBOSE)) {
      FLog.v(
          TAG,
          "time %d: onRequestSubmit: {requestId: %s, callerContext: %s, isPrefetch: %b}",
          getTime(),
          requestId,
          callerContextObject,
          isPrefetch);
      mRequestStartTimeMap.put(requestId, getTime());
    }
  }

  @Override
  public synchronized void onProducerStart(String requestId, String producerName) {
    if (FLog.isLoggable(FLog.VERBOSE)) {
      Pair<String, String> mapKey = Pair.create(requestId, producerName);
      long startTime = getTime();
      mProducerStartTimeMap.put(mapKey, startTime);
      FLog.v(
          TAG,
          "time %d: onProducerStart: {requestId: %s, producer: %s}",
          startTime,
          requestId,
          producerName);
    }
  }

  @Override
  public synchronized void onProducerFinishWithSuccess(
      String requestId,
      String producerName,
      @Nullable Map<String, String> extraMap) {
    if (FLog.isLoggable(FLog.VERBOSE)) {
      Pair<String, String> mapKey = Pair.create(requestId, producerName);
      Long startTime = mProducerStartTimeMap.remove(mapKey);
      long currentTime = getTime();
      FLog.v(
          TAG,
          "time %d: onProducerFinishWithSuccess: " +
              "{requestId: %s, producer: %s, elapsedTime: %d ms, extraMap: %s}",
          currentTime,
          requestId,
          producerName,
          getElapsedTime(startTime, currentTime),
          extraMap);
    }
  }

  @Override
  public synchronized void onProducerFinishWithFailure(
      String requestId,
      String producerName,
      Throwable throwable,
      @Nullable Map<String, String> extraMap) {
    if (FLog.isLoggable(FLog.WARN)) {
      Pair<String, String> mapKey = Pair.create(requestId, producerName);
      Long startTime = mProducerStartTimeMap.remove(mapKey);
      long currentTime = getTime();
      FLog.w(
          TAG,
          "time %d: onProducerFinishWithFailure: " +
              "{requestId: %s, stage: %s, elapsedTime: %d ms, extraMap: %s, throwable: %s}",
          currentTime,
          requestId,
          producerName,
          getElapsedTime(startTime, currentTime),
          extraMap,
          throwable.toString());
    }
  }

  @Override
  public synchronized void onProducerFinishWithCancellation(
      String requestId,
      String producerName,
      @Nullable Map<String, String> extraMap) {
    if (FLog.isLoggable(FLog.VERBOSE)) {
      Pair<String, String> mapKey = Pair.create(requestId, producerName);
      Long startTime = mProducerStartTimeMap.remove(mapKey);
      long currentTime = getTime();
      FLog.v(
          TAG,
          "time %d: onProducerFinishWithCancellation: " +
              "{requestId: %s, stage: %s, elapsedTime: %d ms, extraMap: %s}",
          currentTime,
          requestId,
          producerName,
          getElapsedTime(startTime, currentTime),
          extraMap);
    }
  }

  @Override
  public synchronized void onProducerEvent(
      String requestId, String producerName, String producerEventName) {
    if (FLog.isLoggable(FLog.VERBOSE)) {
      Pair<String, String> mapKey = Pair.create(requestId, producerName);
      Long startTime = mProducerStartTimeMap.get(mapKey);
      long currentTime = getTime();
      FLog.v(
          TAG,
          "time %d: onProducerEvent: {requestId: %s, stage: %s, eventName: %s; elapsedTime: %d ms}",
          getTime(),
          requestId,
          producerName,
          producerEventName,
          getElapsedTime(startTime, currentTime));
    }
  }

  @Override
  public synchronized void onRequestSuccess(
      ImageRequest request,
      String requestId,
      boolean isPrefetch) {
    if (FLog.isLoggable(FLog.VERBOSE)) {
      Long startTime = mRequestStartTimeMap.remove(requestId);
      long currentTime = getTime();
      FLog.v(
          TAG,
          "time %d: onRequestSuccess: {requestId: %s, elapsedTime: %d ms}",
          currentTime,
          requestId,
          getElapsedTime(startTime, currentTime));
    }
  }

  @Override
  public synchronized void onRequestFailure(
      ImageRequest request,
      String requestId,
      Throwable throwable,
      boolean isPrefetch) {
    if (FLog.isLoggable(FLog.WARN)) {
      Long startTime = mRequestStartTimeMap.remove(requestId);
      long currentTime = getTime();
      FLog.w(
          TAG,
          "time %d: onRequestFailure: {requestId: %s, elapsedTime: %d ms, throwable: %s}",
          currentTime,
          requestId,
          getElapsedTime(startTime, currentTime),
          throwable.toString());
    }
  }

  @Override
  public synchronized void onRequestCancellation(String requestId) {
    if (FLog.isLoggable(FLog.VERBOSE)) {
      Long startTime = mRequestStartTimeMap.remove(requestId);
      long currentTime = getTime();
      FLog.v(
          TAG,
          "time %d: onRequestCancellation: {requestId: %s, elapsedTime: %d ms}",
          currentTime,
          requestId,
          getElapsedTime(startTime, currentTime));
    }
  }

  @Override
  public boolean requiresExtraMap(String id) {
    return FLog.isLoggable(FLog.VERBOSE);
  }

  private static long getElapsedTime(@Nullable Long startTime, long endTime) {
    if (startTime != null) {
      return endTime - startTime;
    }
    return -1;
  }

  private static long getTime() {
    return SystemClock.elapsedRealtime();
  }
}

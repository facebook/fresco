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

import java.util.Map;

import com.facebook.imagepipeline.request.ImageRequest;

public class BaseRequestListener implements RequestListener {

  @Override
  public void onRequestStart(
      ImageRequest request, Object callerContext, String requestId, boolean isPrefetch) {
  }

  @Override
  public void onRequestSuccess(
      ImageRequest request, String requestId, boolean isPrefetch) {
  }

  @Override
  public void onRequestFailure(
      ImageRequest request, String requestId, Throwable throwable, boolean isPrefetch) {
  }

  @Override
  public void onRequestCancellation(String requestId) {
  }

  @Override
  public void onProducerStart(String requestId, String producerName) {
  }

  @Override
  public void onProducerEvent(String requestId, String producerName, String eventName) {
  }

  @Override
  public void onProducerFinishWithSuccess(
      String requestId, String producerName, @Nullable Map<String, String> extraMap) {
  }

  @Override
  public void onProducerFinishWithFailure(
      String requestId,
      String producerName,
      Throwable t,
      @Nullable Map<String, String> extraMap) {
  }

  @Override
  public void onProducerFinishWithCancellation(
      String requestId, String producerName, @Nullable Map<String, String> extraMap) {
  }

  @Override
  public boolean requiresExtraMap(String requestId) {
    return false;
  }
}

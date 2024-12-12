/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.flipper.plugins.fresco;

import com.facebook.imagepipeline.debug.DebugImageTracker;
import com.facebook.imagepipeline.listener.BaseRequestListener;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.infer.annotation.Nullsafe;

/** Fresco image {@link RequestListener} that logs events for Sonar. */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class FrescoFlipperRequestListener extends BaseRequestListener {

  private final DebugImageTracker mDebugImageTracker;

  public FrescoFlipperRequestListener(DebugImageTracker debugImageTracker) {
    mDebugImageTracker = debugImageTracker;
  }

  @Override
  public void onRequestStart(
      ImageRequest request, Object callerContext, String requestId, boolean isPrefetch) {
    mDebugImageTracker.trackImageRequest(request, requestId);
  }
}

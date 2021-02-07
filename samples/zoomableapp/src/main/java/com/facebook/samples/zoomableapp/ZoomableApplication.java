/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.zoomableapp;

import android.app.Application;
import com.facebook.common.logging.FLog;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.imagepipeline.listener.RequestLoggingListener;
import java.util.HashSet;
import java.util.Set;

/** Zoomable Application implementation where we set up Fresco */
public class ZoomableApplication extends Application {
  @Override
  public void onCreate() {
    super.onCreate();
    FLog.setMinimumLoggingLevel(FLog.VERBOSE);
    Set<RequestListener> listeners = new HashSet<>();
    listeners.add(new RequestLoggingListener());
    ImagePipelineConfig config =
        ImagePipelineConfig.newBuilder(this).setRequestListeners(listeners).build();
    Fresco.initialize(this, config);
  }
}

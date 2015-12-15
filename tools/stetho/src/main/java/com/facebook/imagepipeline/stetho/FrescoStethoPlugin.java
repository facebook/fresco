/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.stetho;

import com.facebook.drawee.backends.pipeline.Fresco;

/**
 * Use this plugin to allow Stetho to examine the contents of Fresco's caches.
 *
 * Before running the dumpapp script from the command line, the app must
 * already have called {@link Stetho#initialize} and {@link Fresco#initialize}.
 */
public class FrescoStethoPlugin extends BaseFrescoStethoPlugin {

  public FrescoStethoPlugin() {
  }

  protected void ensureInitialized() {
    if (!mInitialized) {
      initialize(Fresco.getImagePipelineFactory());
    }
  }
}

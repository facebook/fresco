/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
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

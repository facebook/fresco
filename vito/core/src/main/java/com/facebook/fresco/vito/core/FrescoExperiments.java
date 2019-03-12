/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core;

import javax.annotation.Nullable;

/** Fresco experiment class with default values. Override this class to change them. */
public class FrescoExperiments {

  public boolean prepareImagePipelineComponents() {
    return false;
  }

  public boolean checkCacheInAttach() {
    return false;
  }

  public int mountContentPoolSize() {
    // Default Litho mount pool size
    return 3;
  }

  public boolean mountContentPoolSync() {
    // Default Litho mount pool sync value
    return true;
  }

  public boolean prefetchInOnPrepare() {
    return false;
  }

  public boolean keepRefToPrefetchDatasouce() {
    return false;
  }

  public boolean prepareActualImageWrapperInBackground() {
    return false;
  }

  public boolean preparePlaceholderDrawableInBackground() {
    return false;
  }

  public boolean keepRefToMainFetchDatasouce() {
    return false;
  }

  public @Nullable Boolean prefetchToBitmapCache() {
    return null;
  }

  public boolean closeDatasource() {
    return false;
  }

  public boolean cacheImageInState() {
    return true;
  }

  public boolean shouldDiffCallerContext() {
    return true;
  }

  public void setupPropDiffingExperiment() {}
}

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
    return true;
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
    return true;
  }

  public boolean preparePlaceholderDrawableInBackground() {
    return false;
  }

  public boolean keepRefToMainFetchDatasouce() {
    return true;
  }

  public @Nullable Boolean prefetchToBitmapCache() {
    return null;
  }

  public boolean closeDatasource() {
    return true;
  }

  public boolean cacheImageInState() {
    return false;
  }

  public boolean shouldDiffCallerContext() {
    return false;
  }

  public void setupPropDiffingExperiment() {}

  public boolean fadeInImages() {
    return false;
  }
}

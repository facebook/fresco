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

  public boolean checkStateCacheInAttach() {
    return true;
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

  public boolean useFetchApiForPrefetch() {
    return true;
  }

  public boolean enqueuePrefetchInOnPrepare() {
    return false;
  }

  public PrefetchTarget onPreparePrefetchTarget() {
    return PrefetchTarget.MEMORY_DECODED;
  }

  public boolean keepRefToPrefetchDatasource() {
    return false;
  }

  public boolean prepareActualImageWrapperInBackground() {
    return true;
  }

  public boolean preparePlaceholderDrawableInBackground() {
    return false;
  }

  public boolean keepRefToMainFetchDatasource() {
    return true;
  }

  public @Nullable Boolean prefetchToBitmapCache() {
    return null;
  }

  public boolean closeDatasource() {
    return true;
  }

  public boolean closePrefetchDataSource() {
    return false;
  }

  public boolean cacheImageInState() {
    return false;
  }

  public boolean shouldDiffCallerContext() {
    return false;
  }

  public void setupPropDiffingExperiment() {}

  public boolean enablePropDiffing() {
    return true;
  }

  public boolean useBindCallbacks() {
    return false;
  }

  public boolean releaseInDetach() {
    return false;
  }

  public boolean releaseInUnmount() {
    return true;
  }

  public boolean delayedReleaseInUnbind() {
    return false;
  }

  public boolean useMountContentOverState() {
    return false;
  }

  public boolean resetState() {
    return false;
  }

  public boolean closeDatasourceOnNewResult() {
    return true;
  }

  public boolean useNativeRounding() {
    return true;
  }

  public boolean useFastNativeRounding() {
    return false;
  }

  public boolean fireOffRequestInBackground() {
    return true;
  }

  public boolean enableWorkingRangePrefetching() {
    return false;
  }

  public int workingRangePrefetchingSize() {
    return 3;
  }

  public PrefetchTarget workingRangePrefetchTarget() {
    return PrefetchTarget.MEMORY_DECODED;
  }
}

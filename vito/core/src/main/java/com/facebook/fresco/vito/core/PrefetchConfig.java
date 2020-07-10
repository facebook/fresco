/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core;

public interface PrefetchConfig {

  boolean prefetchInOnPrepare();

  PrefetchTarget prefetchTargetOnPrepare();

  boolean cancelOnPreparePrefetchWhenWorkingRangePrefetch();

  boolean cancelPrefetchWhenFetched();

  boolean prefetchWithWorkingRange();

  int prefetchWorkingRangeSize();

  PrefetchTarget prefetchTargetWorkingRange();
}

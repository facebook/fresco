/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core;

public class DefaultFrescoVitoConfig implements FrescoVitoConfig {

  private final PrefetchConfig mPrefetchConfig = new DefaultPrefetchConfig();

  @Override
  public PrefetchConfig getPrefetchConfig() {
    return mPrefetchConfig;
  }

  @Override
  public boolean submitFetchOnBgThread() {
    return true;
  }

  public static class DefaultPrefetchConfig implements PrefetchConfig {
    @Override
    public boolean prefetchInOnPrepare() {
      return true;
    }

    @Override
    public PrefetchTarget prefetchTargetOnPrepare() {
      return PrefetchTarget.MEMORY_DECODED;
    }

    @Override
    public boolean cancelOnPreparePrefetchWhenWorkingRangePrefetch() {
      return true;
    }

    @Override
    public boolean cancelPrefetchWhenFetched() {
      return true;
    }

    @Override
    public boolean prefetchWithWorkingRange() {
      return true;
    }

    @Override
    public int prefetchWorkingRangeSize() {
      return 3;
    }

    @Override
    public PrefetchTarget prefetchTargetWorkingRange() {
      return PrefetchTarget.MEMORY_DECODED;
    }
  }
}

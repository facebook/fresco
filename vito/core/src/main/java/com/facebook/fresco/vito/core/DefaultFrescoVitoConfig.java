/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core;

import com.facebook.common.internal.Supplier;
import com.facebook.common.internal.Suppliers;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.infer.annotation.OkToExtend;

@Nullsafe(Nullsafe.Mode.STRICT)
@OkToExtend
public class DefaultFrescoVitoConfig implements FrescoVitoConfig {

  private final PrefetchConfig mPrefetchConfig;

  public DefaultFrescoVitoConfig() {
    this(new DefaultPrefetchConfig());
  }

  public DefaultFrescoVitoConfig(PrefetchConfig prefetchConfig) {
    mPrefetchConfig = prefetchConfig;
  }

  @Override
  public PrefetchConfig getPrefetchConfig() {
    return mPrefetchConfig;
  }

  @Override
  public boolean submitFetchOnBgThread() {
    return true;
  }

  @Override
  public boolean useBindOnly() {
    return false;
  }

  @Override
  public boolean useNewReleaseCallback() {
    return false;
  }

  @Override
  public Supplier<Boolean> useNativeRounding() {
    return Suppliers.BOOLEAN_TRUE;
  }

  @Override
  public boolean layoutPrefetchingEnabled(Object surface) {
    return false;
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

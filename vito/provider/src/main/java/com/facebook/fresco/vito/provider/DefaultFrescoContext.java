/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.provider;

import com.facebook.callercontext.CallerContextVerifier;
import com.facebook.common.executors.UiThreadImmediateExecutorService;
import com.facebook.fresco.vito.core.FrescoContext;
import com.facebook.fresco.vito.core.FrescoExperiments;
import com.facebook.fresco.vito.core.HierarcherImpl;
import javax.annotation.Nullable;

public class DefaultFrescoContext {

  private static @Nullable FrescoContext sInstance;

  public static synchronized FrescoContext get() {
    if (sInstance == null) {
      sInstance = createDefaultContext();
    }
    return sInstance;
  }

  public static synchronized void set(FrescoContext context) {
    if (sInstance != null) {
      throw new RuntimeException("Fresco has already been initialized!");
    }
    sInstance = context;
  }

  public static synchronized boolean isInitialized() {
    return sInstance != null;
  }

  private static synchronized FrescoContext createDefaultContext() {
    return new FrescoContext(
        new HierarcherImpl(),
        new NoOpCallerContextVerifier(),
        new FrescoExperiments(),
        UiThreadImmediateExecutorService.getInstance(),
        null);
  }

  private static class NoOpCallerContextVerifier implements CallerContextVerifier {

    @Override
    public void verifyCallerContext(@Nullable Object callerContext) {
      // No-op
    }
  }
}

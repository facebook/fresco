/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.provider;

import android.content.res.Resources;
import com.facebook.callercontext.CallerContextVerifier;
import com.facebook.common.executors.UiThreadImmediateExecutorService;
import com.facebook.common.internal.Supplier;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.fresco.vito.core.FrescoContext;
import com.facebook.fresco.vito.core.FrescoExperiments;
import com.facebook.fresco.vito.core.HierarcherImpl;
import com.facebook.fresco.vito.core.debug.DefaultDebugOverlayFactory;
import com.facebook.fresco.vito.core.debug.NoOpDebugOverlayFactory;
import com.facebook.fresco.vito.drawable.ArrayVitoDrawableFactory;
import com.facebook.fresco.vito.drawable.BitmapDrawableFactory;
import com.facebook.fresco.vito.drawable.VitoDrawableFactory;
import com.facebook.fresco.vito.draweesupport.DrawableFactoryWrapper;
import javax.annotation.Nullable;

public class DefaultFrescoContext {

  private static @Nullable FrescoContext sInstance;
  private static @Nullable Supplier<Boolean> sDebugOverlayEnabledSupplier;

  public static synchronized FrescoContext get(Resources resources) {
    if (sInstance == null) {
      initialize(resources, null);
    }
    return sInstance;
  }

  public static synchronized void set(FrescoContext context) {
    if (sInstance != null) {
      throw new RuntimeException("Fresco has already been initialized!");
    }
    sInstance = context;
  }

  public static synchronized void initialize(
      Resources resources, @Nullable FrescoExperiments frescoExperiments) {
    set(createDefaultContext(resources, frescoExperiments));
  }

  public static synchronized void setDebugOverlayEnabledSupplier(
      @Nullable Supplier<Boolean> debugOverlayEnabledSupplier) {
    sDebugOverlayEnabledSupplier = debugOverlayEnabledSupplier;
  }

  public static synchronized boolean isInitialized() {
    return sInstance != null;
  }

  private static synchronized FrescoContext createDefaultContext(
      Resources resources, @Nullable FrescoExperiments frescoExperiments) {
    FrescoExperiments actualFrescoExperiments =
        frescoExperiments != null ? frescoExperiments : new FrescoExperiments();
    return new FrescoContext(
        new HierarcherImpl(createDefaultDrawableFactory(resources, actualFrescoExperiments)),
        new NoOpCallerContextVerifier(),
        actualFrescoExperiments,
        UiThreadImmediateExecutorService.getInstance(),
        null,
        sDebugOverlayEnabledSupplier == null
            ? new NoOpDebugOverlayFactory()
            : new DefaultDebugOverlayFactory(sDebugOverlayEnabledSupplier));
  }

  private static VitoDrawableFactory createDefaultDrawableFactory(
      Resources resources, FrescoExperiments frescoExperiments) {
    return new ArrayVitoDrawableFactory(
        new BitmapDrawableFactory(resources, frescoExperiments),
        new DrawableFactoryWrapper(
            Fresco.getImagePipelineFactory().getAnimatedDrawableFactory(null)));
  }

  private static class NoOpCallerContextVerifier implements CallerContextVerifier {

    @Override
    public void verifyCallerContext(@Nullable Object callerContext, boolean isPrefetch) {
      // No-op
    }
  }
}

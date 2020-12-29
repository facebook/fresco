/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.provider.impl;

import android.content.res.Resources;
import com.facebook.callercontext.CallerContextVerifier;
import com.facebook.common.executors.UiThreadImmediateExecutorService;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.Supplier;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.fresco.vito.core.FrescoContext;
import com.facebook.fresco.vito.core.FrescoExperiments;
import com.facebook.fresco.vito.core.impl.FrescoContextImpl;
import com.facebook.fresco.vito.core.impl.HierarcherImpl;
import com.facebook.fresco.vito.core.impl.debug.DefaultDebugOverlayFactory;
import com.facebook.fresco.vito.core.impl.debug.NoOpDebugOverlayFactory;
import com.facebook.fresco.vito.drawable.ArrayVitoDrawableFactory;
import com.facebook.fresco.vito.drawable.BitmapDrawableFactory;
import com.facebook.fresco.vito.draweesupport.DrawableFactoryWrapper;
import com.facebook.fresco.vito.options.ImageOptionsDrawableFactory;
import com.facebook.imagepipeline.drawable.DrawableFactory;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class DefaultFrescoContext {

  private static @Nullable FrescoContext sInstance;
  private static @Nullable Supplier<Boolean> sDebugOverlayEnabledSupplier;

  public static synchronized FrescoContext get(Resources resources) {
    if (sInstance == null) {
      initialize(resources, null);
    }
    return Preconditions.checkNotNull(sInstance);
  }

  public static synchronized FrescoContext get() {
    if (sInstance == null) {
      throw new RuntimeException("FrescoContext must be initialized!");
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

  /**
   * Shut down the default Fresco context if no longer needed or if it needs to be re-initialized.
   */
  public static synchronized void shutdown() {
    sInstance = null;
  }

  public static synchronized void setDebugOverlayEnabledSupplier(
      @Nullable Supplier<Boolean> debugOverlayEnabledSupplier) {
    sDebugOverlayEnabledSupplier = debugOverlayEnabledSupplier;
  }

  @Nullable
  public static Supplier<Boolean> getDebugOverlayEnabledSupplier() {
    return sDebugOverlayEnabledSupplier;
  }

  public static synchronized boolean isInitialized() {
    return sInstance != null;
  }

  private static synchronized FrescoContext createDefaultContext(
      Resources resources, @Nullable FrescoExperiments frescoExperiments) {
    FrescoExperiments actualFrescoExperiments =
        frescoExperiments != null ? frescoExperiments : new FrescoExperiments();
    return new FrescoContextImpl(
        new HierarcherImpl(createDefaultDrawableFactory(resources, actualFrescoExperiments)),
        new NoOpCallerContextVerifier(),
        actualFrescoExperiments,
        UiThreadImmediateExecutorService.getInstance(),
        Fresco.getImagePipeline().getConfig().getExecutorSupplier().forLightweightBackgroundTasks(),
        null,
        null,
        null,
        sDebugOverlayEnabledSupplier == null
            ? new NoOpDebugOverlayFactory()
            : new DefaultDebugOverlayFactory(sDebugOverlayEnabledSupplier));
  }

  private static ImageOptionsDrawableFactory createDefaultDrawableFactory(
      Resources resources, FrescoExperiments frescoExperiments) {
    DrawableFactory animatedDrawableFactory =
        Fresco.getImagePipelineFactory().getAnimatedDrawableFactory(null);
    return new ArrayVitoDrawableFactory(
        new BitmapDrawableFactory(resources, frescoExperiments),
        animatedDrawableFactory == null
            ? null
            : new DrawableFactoryWrapper(animatedDrawableFactory));
  }

  private static class NoOpCallerContextVerifier implements CallerContextVerifier {

    @Override
    public void verifyCallerContext(@Nullable Object callerContext, boolean isPrefetch) {
      // No-op
    }
  }
}

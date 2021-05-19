/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.provider.impl;

import android.content.res.Resources;
import com.facebook.callercontext.CallerContextVerifier;
import com.facebook.common.internal.Supplier;
import com.facebook.fresco.vito.core.DefaultFrescoVitoConfig;
import com.facebook.fresco.vito.core.FrescoController2;
import com.facebook.fresco.vito.core.FrescoVitoConfig;
import com.facebook.fresco.vito.core.FrescoVitoPrefetcher;
import com.facebook.fresco.vito.core.ImagePipelineUtils;
import com.facebook.fresco.vito.core.VitoImagePipeline;
import com.facebook.fresco.vito.core.impl.FrescoController2Impl;
import com.facebook.fresco.vito.core.impl.FrescoVitoPrefetcherImpl;
import com.facebook.fresco.vito.core.impl.HierarcherImpl;
import com.facebook.fresco.vito.core.impl.NoOpVitoImagePerfListener;
import com.facebook.fresco.vito.core.impl.VitoImagePipelineImpl;
import com.facebook.fresco.vito.core.impl.debug.DefaultDebugOverlayFactory2;
import com.facebook.fresco.vito.core.impl.debug.NoOpDebugOverlayFactory2;
import com.facebook.fresco.vito.drawable.ArrayVitoDrawableFactory;
import com.facebook.fresco.vito.drawable.BitmapDrawableFactory;
import com.facebook.fresco.vito.draweesupport.DrawableFactoryWrapper;
import com.facebook.fresco.vito.options.ImageOptionsDrawableFactory;
import com.facebook.fresco.vito.provider.FrescoVitoProvider;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.core.ImagePipelineFactory;
import com.facebook.imagepipeline.drawable.DrawableFactory;
import com.facebook.infer.annotation.Nullsafe;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class DefaultFrescoVitoProvider implements FrescoVitoProvider.Implementation {

  private final FrescoController2 mFrescoController;
  private final VitoImagePipeline mVitoImagePipeline;
  private final FrescoVitoPrefetcher mFrescoVitoPrefetcher;
  private final FrescoVitoConfig mFrescoVitoConfig;

  public DefaultFrescoVitoProvider(
      final Resources resources,
      final ImagePipeline imagePipeline,
      final Executor lightweightBackgroundThreadExecutor,
      final Executor uiThreadExecutor,
      final ImagePipelineUtils imagePipelineUtils,
      final @Nullable Supplier<Boolean> debugOverlayEnabledSupplier) {
    this(
        resources,
        new DefaultFrescoVitoConfig(),
        imagePipeline,
        imagePipelineUtils,
        lightweightBackgroundThreadExecutor,
        uiThreadExecutor,
        debugOverlayEnabledSupplier,
        new NoOpCallerContextVerifier());
  }

  public DefaultFrescoVitoProvider(
      Resources resources,
      FrescoVitoConfig config,
      ImagePipeline imagePipeline,
      ImagePipelineUtils imagePipelineUtils,
      Executor lightweightBackgroundThreadExecutor,
      Executor uiThreadExecutor,
      @Nullable Supplier<Boolean> debugOverlayEnabledSupplier,
      CallerContextVerifier callerContextVerifier) {
    if (!ImagePipelineFactory.hasBeenInitialized()) {
      throw new RuntimeException(
          "Fresco must be initialized before DefaultFrescoVitoProvider can be used!");
    }
    mFrescoVitoConfig = config;
    mFrescoVitoPrefetcher =
        new FrescoVitoPrefetcherImpl(imagePipeline, imagePipelineUtils, callerContextVerifier);
    mVitoImagePipeline = new VitoImagePipelineImpl(imagePipeline, imagePipelineUtils);
    mFrescoController =
        new FrescoController2Impl(
            mFrescoVitoConfig,
            new HierarcherImpl(createDefaultDrawableFactory(resources)),
            lightweightBackgroundThreadExecutor,
            uiThreadExecutor,
            mVitoImagePipeline,
            null,
            debugOverlayEnabledSupplier == null
                ? new NoOpDebugOverlayFactory2()
                : new DefaultDebugOverlayFactory2(debugOverlayEnabledSupplier),
            null,
            new NoOpVitoImagePerfListener());
  }

  @Override
  public FrescoController2 getController() {
    return mFrescoController;
  }

  @Override
  public FrescoVitoPrefetcher getPrefetcher() {
    return mFrescoVitoPrefetcher;
  }

  @Override
  public VitoImagePipeline getImagePipeline() {
    return mVitoImagePipeline;
  }

  @Override
  public FrescoVitoConfig getConfig() {
    return mFrescoVitoConfig;
  }

  private static ImageOptionsDrawableFactory createDefaultDrawableFactory(Resources resources) {
    DrawableFactory animatedDrawableFactory =
        ImagePipelineFactory.getInstance().getAnimatedDrawableFactory(null);
    return new ArrayVitoDrawableFactory(
        new BitmapDrawableFactory(resources),
        animatedDrawableFactory == null
            ? null
            : new DrawableFactoryWrapper(animatedDrawableFactory));
  }
}

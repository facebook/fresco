/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.provider;

import com.facebook.fresco.vito.core.DefaultFrescoVitoConfig;
import com.facebook.fresco.vito.core.FrescoContext;
import com.facebook.fresco.vito.core.FrescoController2;
import com.facebook.fresco.vito.core.FrescoVitoConfig;
import com.facebook.fresco.vito.core.FrescoVitoPrefetcher;
import com.facebook.fresco.vito.core.VitoImagePipeline;
import com.facebook.fresco.vito.core.impl.FrescoController2Impl;
import com.facebook.fresco.vito.core.impl.VitoImagePipelineImpl;
import com.facebook.imagepipeline.core.ImagePipelineFactory;

public class DefaultFrescoVitoProvider implements FrescoVitoProvider.Implementation {

  private FrescoController2 mFrescoController;
  private VitoImagePipeline mVitoImagePipeline;
  private FrescoVitoPrefetcher mFrescoVitoPrefetcher;
  private FrescoVitoConfig mFrescoVitoConfig;

  public DefaultFrescoVitoProvider() {
    this(DefaultFrescoContext.get(), new DefaultFrescoVitoConfig());
  }

  public DefaultFrescoVitoProvider(FrescoVitoConfig config) {
    this(DefaultFrescoContext.get(), config);
  }

  public DefaultFrescoVitoProvider(FrescoContext context, FrescoVitoConfig config) {
    if (!ImagePipelineFactory.hasBeenInitialized()) {
      throw new RuntimeException(
          "Fresco must be initialized before DefaultFrescoVitoProvider can be used!");
    }
    mFrescoVitoConfig = config;
    mFrescoVitoPrefetcher = context.getPrefetcher();
    mVitoImagePipeline =
        new VitoImagePipelineImpl(context.getImagePipeline(), context.getImagePipelineUtils());
    mFrescoController =
        new FrescoController2Impl(
            context.getHierarcher(),
            context.getLightweightBackgroundThreadExecutor(),
            context.getUiThreadExecutorService(),
            mVitoImagePipeline,
            null);
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
}

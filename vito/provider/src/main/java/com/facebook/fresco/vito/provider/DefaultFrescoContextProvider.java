/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.provider;

import com.facebook.fresco.vito.core.FrescoContext;
import com.facebook.fresco.vito.core.FrescoController2;
import com.facebook.fresco.vito.core.FrescoVitoPrefetcher;
import com.facebook.fresco.vito.core.VitoImagePipeline;
import com.facebook.imagepipeline.core.ImagePipelineFactory;

public class DefaultFrescoContextProvider implements FrescoContextProvider.Implementation {

  private FrescoController2 mFrescoController;
  private VitoImagePipeline mVitoImagePipeline;
  private FrescoVitoPrefetcher mFrescoVitoPrefetcher;

  public DefaultFrescoContextProvider() {
    this(DefaultFrescoContext.get());
  }

  public DefaultFrescoContextProvider(FrescoContext context) {
    if (!ImagePipelineFactory.hasBeenInitialized()) {
      throw new RuntimeException(
          "Fresco must be initialized before DefaultFrescoContextProvider can be used!");
    }
    mFrescoVitoPrefetcher = context.getPrefetcher();
    mVitoImagePipeline =
        new VitoImagePipeline(context.getImagePipeline(), context.getImagePipelineUtils());
    mFrescoController =
        new FrescoController2(
            context.getHierarcher(),
            context.getLightweightBackgroundThreadExecutor(),
            context.getUiThreadExecutorService(),
            mVitoImagePipeline);
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
}

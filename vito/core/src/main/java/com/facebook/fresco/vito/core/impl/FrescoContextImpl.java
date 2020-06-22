/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl;

import com.facebook.callercontext.CallerContextVerifier;
import com.facebook.fresco.ui.common.ControllerListener2;
import com.facebook.fresco.vito.core.FrescoContext;
import com.facebook.fresco.vito.core.FrescoController;
import com.facebook.fresco.vito.core.FrescoExperiments;
import com.facebook.fresco.vito.core.FrescoVitoPrefetcher;
import com.facebook.fresco.vito.core.Hierarcher;
import com.facebook.fresco.vito.core.ImagePipelineUtils;
import com.facebook.fresco.vito.core.ImageStateListener;
import com.facebook.fresco.vito.core.impl.debug.DebugOverlayFactory;
import com.facebook.fresco.vito.listener.ImageListener;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.core.ImagePipelineFactory;
import com.facebook.infer.annotation.ThreadSafe;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;

@ThreadSafe
public class FrescoContextImpl implements FrescoContext {

  private final @Nullable CallerContextVerifier mCallerContextVerifier;
  private final FrescoExperiments mExperiments;
  private final @Nullable ImageListener mGlobalImageListener;
  private final @Nullable ImageStateListener mGlobalImageStateListener;
  private final Hierarcher mHierarcher;
  private final Executor mUiThreadExecutor;
  private final Executor mLightweightBackgroundThreadExecutor;
  private final ImagePipelineUtils mImagePipelineUtils;

  private FrescoController mController;
  private FrescoVitoPrefetcher mPrefetcher;

  private @Nullable ImagePipelineFactory mImagePipelineFactory;

  public FrescoContextImpl(
      FrescoController controller,
      Hierarcher hierarcher,
      @Nullable CallerContextVerifier callerContextVerifier,
      FrescoExperiments frescoExperiments,
      Executor uiThreadExecutor,
      Executor lightweightBackgroundThreadExecutor,
      @Nullable ImageListener globalImageListener,
      @Nullable ImageStateListener globalImageStateListener) {
    mController = controller;
    mHierarcher = hierarcher;
    mCallerContextVerifier = callerContextVerifier;
    mExperiments = frescoExperiments;
    mUiThreadExecutor = uiThreadExecutor;
    mGlobalImageListener = globalImageListener;
    mLightweightBackgroundThreadExecutor = lightweightBackgroundThreadExecutor;
    mGlobalImageStateListener = globalImageStateListener;
    mImagePipelineUtils = new ImagePipelineUtilsImpl(mExperiments);
  }

  public FrescoContextImpl(
      Hierarcher hierarcher,
      @Nullable CallerContextVerifier callerContextVerifier,
      FrescoExperiments frescoExperiments,
      Executor uiThreadExecutor,
      Executor lightweightBackgroundThreadExecutor,
      @Nullable ImageListener globalImageListener,
      @Nullable ImageStateListener globalImageStateListener,
      @Nullable ControllerListener2 imagePerfControllerListener2,
      DebugOverlayFactory debugOverlayFactory) {
    mController =
        new FrescoControllerImpl(this, debugOverlayFactory, false, imagePerfControllerListener2);
    mHierarcher = hierarcher;
    mCallerContextVerifier = callerContextVerifier;
    mExperiments = frescoExperiments;
    mUiThreadExecutor = uiThreadExecutor;
    mGlobalImageListener = globalImageListener;
    mGlobalImageStateListener = globalImageStateListener;
    mLightweightBackgroundThreadExecutor = lightweightBackgroundThreadExecutor;
    mImagePipelineUtils = new ImagePipelineUtilsImpl(mExperiments);
  }

  @Override
  public ImagePipelineFactory getImagePipelineFactory() {
    if (mImagePipelineFactory == null) {
      mImagePipelineFactory = ImagePipelineFactory.getInstance();
    }
    return mImagePipelineFactory;
  }

  @Override
  public void setImagePipelineFactory(@Nullable ImagePipelineFactory imagePipelineFactory) {
    mImagePipelineFactory = imagePipelineFactory;
  }

  @Override
  public Hierarcher getHierarcher() {
    return mHierarcher;
  }

  @Override
  public ImagePipeline getImagePipeline() {
    return getImagePipelineFactory().getImagePipeline();
  }

  @Override
  public FrescoController getController() {
    return mController;
  }

  @Override
  public FrescoExperiments getExperiments() {
    return mExperiments;
  }

  @Override
  @Nullable
  public ImageListener getGlobalImageListener() {
    return mGlobalImageListener;
  }

  @Override
  public ImageStateListener getGlobalImageStateListener() {
    return mGlobalImageStateListener;
  }

  @Override
  public FrescoVitoPrefetcher getPrefetcher() {
    if (mPrefetcher == null) {
      mPrefetcher = new FrescoVitoPrefetcherImpl(this);
    }
    return mPrefetcher;
  }

  @Override
  public ImagePipelineUtils getImagePipelineUtils() {
    return mImagePipelineUtils;
  }

  @Override
  public void verifyCallerContext(@Nullable Object callerContext) {
    if (mCallerContextVerifier != null) {
      mCallerContextVerifier.verifyCallerContext(callerContext, false);
    }
  }

  @Override
  public Executor getUiThreadExecutorService() {
    return mUiThreadExecutor;
  }

  @Override
  public Executor getLightweightBackgroundThreadExecutor() {
    return mLightweightBackgroundThreadExecutor;
  }

  @Override
  public void setController(FrescoController controller) {
    mController = controller;
  }
}

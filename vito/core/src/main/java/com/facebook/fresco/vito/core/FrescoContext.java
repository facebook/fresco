/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core;

import com.facebook.callercontext.CallerContextVerifier;
import com.facebook.fresco.vito.core.debug.DebugOverlayFactory;
import com.facebook.fresco.vito.listener.ImageListener;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.core.ImagePipelineFactory;
import com.facebook.infer.annotation.ThreadSafe;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nullable;

@ThreadSafe
public class FrescoContext {
  private static final AtomicLong sIdCounter = new AtomicLong();

  private final @Nullable CallerContextVerifier mCallerContextVerifier;
  private final FrescoExperiments mExperiments;
  private final @Nullable ImageListener mGlobalImageListener;
  private final @Nullable ImageStateListener mGlobalImageStateListener;
  private final Hierarcher mHierarcher;
  private final Executor mUiThreadExecutor;
  private final Executor mLightweightBackgroundThreadExecutor;
  private final ImagePipelineUtils mImagePipelineUtils;
  private final VitoImagePipeline mVitoImagePipeline;
  private final FrescoController2 mController2;

  private FrescoController mController;
  private FrescoVitoPrefetcher mPrefetcher;

  private @Nullable ImagePipelineFactory mImagePipelineFactory;

  public FrescoContext(
      FrescoController controller,
      Hierarcher hierarcher,
      @Nullable CallerContextVerifier callerContextVerifier,
      FrescoExperiments frescoExperiments,
      Executor uiThreadExecutor,
      Executor lightweightBackgroundThreadExecutor,
      @Nullable ImageListener globalImageListener,
      @Nullable ImageStateListener globalImageStateListener,
      VitoImagePipeline vitoImagePipeline,
      FrescoController2 frescoController2) {
    mController = controller;
    mHierarcher = hierarcher;
    mCallerContextVerifier = callerContextVerifier;
    mExperiments = frescoExperiments;
    mUiThreadExecutor = uiThreadExecutor;
    mGlobalImageListener = globalImageListener;
    mLightweightBackgroundThreadExecutor = lightweightBackgroundThreadExecutor;
    mGlobalImageStateListener = globalImageStateListener;
    mImagePipelineUtils = new ImagePipelineUtils(mExperiments);
    mVitoImagePipeline = vitoImagePipeline;
    mController2 = frescoController2;
  }

  public FrescoContext(
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
    mImagePipelineUtils = new ImagePipelineUtils(mExperiments);
    mVitoImagePipeline = null;
    mController2 = null;
  }

  public FrescoContext(
      Hierarcher hierarcher,
      @Nullable CallerContextVerifier callerContextVerifier,
      FrescoExperiments frescoExperiments,
      Executor uiThreadExecutor,
      Executor lightweightBackgroundThreadExecutor,
      @Nullable ImageListener globalImageListener,
      @Nullable ImageStateListener globalImageStateListener,
      DebugOverlayFactory debugOverlayFactory) {
    mController = new FrescoControllerImpl(this, debugOverlayFactory, false);
    mHierarcher = hierarcher;
    mCallerContextVerifier = callerContextVerifier;
    mExperiments = frescoExperiments;
    mUiThreadExecutor = uiThreadExecutor;
    mGlobalImageListener = globalImageListener;
    mGlobalImageStateListener = globalImageStateListener;
    mLightweightBackgroundThreadExecutor = lightweightBackgroundThreadExecutor;
    mImagePipelineUtils = new ImagePipelineUtils(mExperiments);
    mVitoImagePipeline = null;
    mController2 = null;
  }

  public ImagePipelineFactory getImagePipelineFactory() {
    if (mImagePipelineFactory == null) {
      mImagePipelineFactory = ImagePipelineFactory.getInstance();
    }
    return mImagePipelineFactory;
  }

  public void setImagePipelineFactory(@Nullable ImagePipelineFactory imagePipelineFactory) {
    mImagePipelineFactory = imagePipelineFactory;
  }

  public Hierarcher getHierarcher() {
    return mHierarcher;
  }

  public ImagePipeline getImagePipeline() {
    return getImagePipelineFactory().getImagePipeline();
  }

  public FrescoController getController() {
    return mController;
  }

  public FrescoController2 getController2() {
    return mController2;
  }

  public FrescoExperiments getExperiments() {
    return mExperiments;
  }

  @Nullable
  public ImageListener getGlobalImageListener() {
    return mGlobalImageListener;
  }

  public ImageStateListener getGlobalImageStateListener() {
    return mGlobalImageStateListener;
  }

  public FrescoVitoPrefetcher getPrefetcher() {
    if (mPrefetcher == null) {
      mPrefetcher = new FrescoVitoPrefetcher(this);
    }
    return mPrefetcher;
  }

  public ImagePipelineUtils getImagePipelineUtils() {
    return mImagePipelineUtils;
  }

  public VitoImagePipeline getVitoImagePipeline() {
    return mVitoImagePipeline;
  }

  public void verifyCallerContext(@Nullable Object callerContext) {
    if (mCallerContextVerifier != null) {
      mCallerContextVerifier.verifyCallerContext(callerContext, false);
    }
  }

  public Executor getUiThreadExecutorService() {
    return mUiThreadExecutor;
  }

  public Executor getLightweightBackgroundThreadExecutor() {
    return mLightweightBackgroundThreadExecutor;
  }

  public void setController(FrescoController controller) {
    mController = controller;
  }

  public static long generateIdentifier() {
    return sIdCounter.incrementAndGet();
  }
}

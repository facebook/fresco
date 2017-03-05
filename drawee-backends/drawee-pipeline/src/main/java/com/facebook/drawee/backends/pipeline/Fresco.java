/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawee.backends.pipeline;

import javax.annotation.Nullable;

import android.content.Context;

import com.facebook.common.logging.FLog;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.imagepipeline.core.ImagePipelineFactory;

/**
 * Fresco entry point.
 *
 * <p/> You must initialize this class before use. The simplest way is to just do
 * {#code Fresco.initialize(Context)}.
 */
public class Fresco {

  private static final Class<?> TAG = Fresco.class;

  private static PipelineDraweeControllerBuilderSupplier sDraweeControllerBuilderSupplier;
  private static volatile boolean sIsInitialized = false;

  private Fresco() {}

  /** Initializes Fresco with the default config. */
  public static void initialize(Context context) {
    initialize(context, null, null);
  }

  /** Initializes Fresco with the default Drawee config. */
  public static void initialize(
      Context context,
      @Nullable ImagePipelineConfig imagePipelineConfig) {
    initialize(context, imagePipelineConfig, null);
  }

  /** Initializes Fresco with the specified config. */
  public static void initialize(
      Context context,
      @Nullable ImagePipelineConfig imagePipelineConfig,
      @Nullable DraweeConfig draweeConfig) {
    if (sIsInitialized) {
      FLog.w(
          TAG,
          "Fresco has already been initialized! `Fresco.initialize(...)` should only be called " +
            "1 single time to avoid memory leaks!");
    } else {
      sIsInitialized = true;
    }
    // we should always use the application context to avoid memory leaks
    context = context.getApplicationContext();
    if (imagePipelineConfig == null) {
      ImagePipelineFactory.initialize(context);
    } else {
      ImagePipelineFactory.initialize(imagePipelineConfig);
    }
    initializeDrawee(context, draweeConfig);
  }

  /** Initializes Drawee with the specified config. */
  private static void initializeDrawee(
      Context context,
      @Nullable DraweeConfig draweeConfig) {
    sDraweeControllerBuilderSupplier =
        new PipelineDraweeControllerBuilderSupplier(context, draweeConfig);
    SimpleDraweeView.initialize(sDraweeControllerBuilderSupplier);
  }

  /** Gets the supplier of Fresco Drawee controller builders. */
  public static PipelineDraweeControllerBuilderSupplier getDraweeControllerBuilderSupplier() {
    return sDraweeControllerBuilderSupplier;
  }

  /** Returns a new instance of Fresco Drawee controller builder. */
  public static PipelineDraweeControllerBuilder newDraweeControllerBuilder() {
    return sDraweeControllerBuilderSupplier.get();
  }

  public static ImagePipelineFactory getImagePipelineFactory() {
    return ImagePipelineFactory.getInstance();
  }

  /** Gets the image pipeline instance. */
  public static ImagePipeline getImagePipeline() {
    return getImagePipelineFactory().getImagePipeline();
  }

  /** Shuts Fresco down. */
  public static void shutDown() {
    sDraweeControllerBuilderSupplier = null;
    SimpleDraweeView.shutDown();
    ImagePipelineFactory.shutDown();
  }

  /** Returns true if Fresco has been initialized. */
  public static boolean hasBeenInitialized() {
    return sIsInitialized;
  }
}

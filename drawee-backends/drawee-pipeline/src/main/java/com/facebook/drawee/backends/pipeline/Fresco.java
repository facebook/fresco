/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawee.backends.pipeline;

import android.content.Context;

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
  private static PipelineDraweeControllerBuilderSupplier sDraweeControllerBuilderSupplier;

  private Fresco() {}

  /** Initializes Fresco with the default config. */
  public static void initialize(Context context) {
    ImagePipelineFactory.initialize(context);
    initializeDrawee(context);
  }

  /** Initializes Fresco with the specified config. */
  public static void initialize(Context context, ImagePipelineConfig imagePipelineConfig) {
    ImagePipelineFactory.initialize(imagePipelineConfig);
    initializeDrawee(context);
  }

  private static void initializeDrawee(Context context) {
    sDraweeControllerBuilderSupplier = new PipelineDraweeControllerBuilderSupplier(context);
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
}

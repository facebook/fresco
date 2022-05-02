/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.backends.pipeline;

import android.content.Context;
import com.facebook.common.logging.FLog;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.imagepipeline.core.ImagePipelineFactory;
import com.facebook.imagepipeline.core.NativeCodeSetup;
import com.facebook.imagepipeline.systrace.FrescoSystrace;
import com.facebook.soloader.nativeloader.NativeLoader;
import com.facebook.soloader.nativeloader.SystemDelegate;
import java.lang.reflect.InvocationTargetException;
import javax.annotation.Nullable;

/**
 * Fresco entry point.
 *
 * <p>You must initialize this class before use. The simplest way is to just do {#code
 * Fresco.initialize(Context)}.
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
      Context context, @Nullable ImagePipelineConfig imagePipelineConfig) {
    initialize(context, imagePipelineConfig, null);
  }

  /** Initializes Fresco with the specified config and native code enabled. */
  public static void initialize(
      Context context,
      @Nullable ImagePipelineConfig imagePipelineConfig,
      @Nullable DraweeConfig draweeConfig) {
    initialize(context, imagePipelineConfig, draweeConfig, true);
  }

  /** Initializes Fresco with the specified config. */
  public static void initialize(
      Context context,
      @Nullable ImagePipelineConfig imagePipelineConfig,
      @Nullable DraweeConfig draweeConfig,
      boolean useNativeCode) {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("Fresco#initialize");
    }
    if (sIsInitialized) {
      FLog.w(
          TAG,
          "Fresco has already been initialized! `Fresco.initialize(...)` should only be called "
              + "1 single time to avoid memory leaks!");
    } else {
      sIsInitialized = true;
    }

    NativeCodeSetup.setUseNativeCode(useNativeCode);

    if (!NativeLoader.isInitialized()) {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.beginSection("Fresco.initialize->SoLoader.init");
      }
      try {
        Class<?> clazz =
            Class.forName("com.facebook.imagepipeline.nativecode.NativeCodeInitializer");
        clazz.getMethod("init", Context.class).invoke(null, context);
      } catch (ClassNotFoundException e) {
        // Failed to initialize SoLoader
        NativeLoader.initIfUninitialized(new SystemDelegate());
      } catch (IllegalAccessException e) {
        // Failed to initialize SoLoader
        NativeLoader.initIfUninitialized(new SystemDelegate());
      } catch (InvocationTargetException e) {
        // Failed to initialize SoLoader
        NativeLoader.initIfUninitialized(new SystemDelegate());
      } catch (NoSuchMethodException e) {
        // Failed to initialize SoLoader
        NativeLoader.initIfUninitialized(new SystemDelegate());
      } finally {
        if (FrescoSystrace.isTracing()) {
          FrescoSystrace.endSection();
        }
      }
    }
    // we should always use the application context to avoid memory leaks
    context = context.getApplicationContext();
    if (imagePipelineConfig == null) {
      ImagePipelineFactory.initialize(context);
    } else {
      ImagePipelineFactory.initialize(imagePipelineConfig);
    }
    initializeDrawee(context, draweeConfig);
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
  }

  /** Initializes Drawee with the specified config. */
  private static void initializeDrawee(Context context, @Nullable DraweeConfig draweeConfig) {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("Fresco.initializeDrawee");
    }
    sDraweeControllerBuilderSupplier =
        new PipelineDraweeControllerBuilderSupplier(context, draweeConfig);
    SimpleDraweeView.initialize(sDraweeControllerBuilderSupplier);
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
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

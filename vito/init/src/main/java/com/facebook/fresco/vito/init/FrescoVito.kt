/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.init

import android.content.res.Resources
import com.facebook.callercontext.CallerContextVerifier
import com.facebook.common.executors.UiThreadImmediateExecutorService
import com.facebook.common.internal.Supplier
import com.facebook.common.internal.Suppliers
import com.facebook.fresco.vito.core.DefaultFrescoVitoConfig
import com.facebook.fresco.vito.core.FrescoVitoConfig
import com.facebook.fresco.vito.core.ImagePipelineUtils
import com.facebook.fresco.vito.core.VitoImagePerfListener
import com.facebook.fresco.vito.core.impl.BaseVitoImagePerfListener
import com.facebook.fresco.vito.core.impl.DefaultImageDecodeOptionsProviderImpl
import com.facebook.fresco.vito.core.impl.ImagePipelineUtilsImpl
import com.facebook.fresco.vito.core.impl.ImagePipelineUtilsImpl.CircularBitmapRounding
import com.facebook.fresco.vito.core.impl.source.ImageSourceProviderImpl
import com.facebook.fresco.vito.nativecode.NativeCircularBitmapRounding
import com.facebook.fresco.vito.provider.FrescoVitoProvider
import com.facebook.fresco.vito.provider.impl.DefaultFrescoVitoProvider
import com.facebook.fresco.vito.provider.impl.NoOpCallerContextVerifier
import com.facebook.fresco.vito.source.ImageSourceProvider
import com.facebook.imagepipeline.core.ImagePipeline
import com.facebook.imagepipeline.core.ImagePipelineFactory
import java.util.concurrent.Executor

class FrescoVito {

  companion object {
    @JvmStatic private var isInitialized = false

    /**
     * Initialize Fresco Vito.
     *
     * Note: Fresco has to be initialized already, via Fresco.initialize(...)
     *
     * @param resources resources for the application
     * @param imagePipeline the image pipeline used for image loading
     * @param debugOverlayEnabledSupplier debug overlay toggle
     */
    @Synchronized
    @JvmOverloads
    @JvmStatic
    fun initialize(
        resources: Resources,
        imagePipeline: ImagePipeline = ImagePipelineFactory.getInstance().imagePipeline,
        lightweightBackgroundThreadExecutor: Executor =
            imagePipeline.config.executorSupplier.forLightweightBackgroundTasks(),
        uiThreadExecutor: Executor = UiThreadImmediateExecutorService.getInstance(),
        debugOverlayEnabledSupplier: Supplier<Boolean?>? = null,
        useNativeCode: Supplier<Boolean> = Suppliers.BOOLEAN_TRUE,
        vitoConfig: FrescoVitoConfig = DefaultFrescoVitoConfig(),
        callerContextVerifier: CallerContextVerifier = NoOpCallerContextVerifier(),
        vitoImagePerfListener: VitoImagePerfListener = BaseVitoImagePerfListener()
    ) {
      if (isInitialized) {
        return
      }
      initialize(
          DefaultFrescoVitoProvider(
              resources,
              vitoConfig,
              imagePipeline,
              createImagePipelineUtils(useNativeCode),
              lightweightBackgroundThreadExecutor,
              uiThreadExecutor,
              debugOverlayEnabledSupplier,
              callerContextVerifier,
              vitoImagePerfListener))
    }

    /**
     * Initialize Fresco Vito. Note: Fresco has to be initialized already, via
     * Fresco.initialize(...)
     *
     * @param providerImplementation the provider implementation to be used
     */
    @Synchronized
    fun initialize(providerImplementation: FrescoVitoProvider.Implementation) {
      if (isInitialized) {
        return
      }
      FrescoVitoProvider.setImplementation(providerImplementation)
      ImageSourceProvider.setImplementation(ImageSourceProviderImpl())
      isInitialized = true
    }

    fun createImagePipelineUtils(
        useNativeRounding: Supplier<Boolean>,
        useFastNativeRounding: Supplier<Boolean> = Suppliers.BOOLEAN_FALSE
    ): ImagePipelineUtils {
      val circularBitmapRounding: CircularBitmapRounding? =
          if (useNativeRounding.get()) NativeCircularBitmapRounding(useFastNativeRounding) else null
      return ImagePipelineUtilsImpl(DefaultImageDecodeOptionsProviderImpl(circularBitmapRounding))
    }
  }
}

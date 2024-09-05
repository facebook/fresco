/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.init

import com.facebook.callercontext.CallerContextVerifier
import com.facebook.common.executors.UiThreadImmediateExecutorService
import com.facebook.common.internal.Supplier
import com.facebook.common.internal.Suppliers
import com.facebook.fresco.vito.core.DefaultFrescoVitoConfig
import com.facebook.fresco.vito.core.FrescoVitoConfig
import com.facebook.fresco.vito.core.ImagePerfLoggingListener
import com.facebook.fresco.vito.core.ImagePipelineUtils
import com.facebook.fresco.vito.core.VitoImagePerfListener
import com.facebook.fresco.vito.core.impl.BaseVitoImagePerfListener
import com.facebook.fresco.vito.core.impl.DefaultImageDecodeOptionsProviderImpl
import com.facebook.fresco.vito.core.impl.ImagePipelineUtilsImpl
import com.facebook.fresco.vito.core.impl.ImagePipelineUtilsImpl.CircularBitmapRounding
import com.facebook.fresco.vito.core.impl.debug.DefaultDebugOverlayFactory2
import com.facebook.fresco.vito.core.impl.debug.NoOpDebugOverlayFactory2
import com.facebook.fresco.vito.nativecode.NativeCircularBitmapRounding
import com.facebook.fresco.vito.provider.components.FrescoVitoComponents
import com.facebook.fresco.vito.provider.impl.DefaultFrescoVitoProvider
import com.facebook.fresco.vito.provider.impl.NoOpCallerContextVerifier
import com.facebook.fresco.vito.provider.setup.FrescoVitoSetup
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
        imagePipeline: ImagePipeline? = null,
        lightweightBackgroundThreadExecutor: Executor? = null,
        uiThreadExecutor: Executor? = null,
        debugOverlayEnabledSupplier: Supplier<Boolean>? = null,
        useNativeCode: Supplier<Boolean> = Suppliers.BOOLEAN_TRUE,
        vitoConfig: FrescoVitoConfig = DefaultFrescoVitoConfig(),
        callerContextVerifier: CallerContextVerifier = NoOpCallerContextVerifier,
        vitoImagePerfListener: VitoImagePerfListener = BaseVitoImagePerfListener(),
        imagePerfListenerSupplier: Supplier<ImagePerfLoggingListener>? = null,
        showExtendedDebugOverlayInformation: Boolean = true,
        showExtendedImageSourceExtraInformation: Boolean = false,
    ) {
      if (isInitialized) {
        return
      }
      val imagePipeline = imagePipeline ?: ImagePipelineFactory.getInstance().imagePipeline
      val lightweightBackgroundThreadExecutor =
          lightweightBackgroundThreadExecutor
              ?: imagePipeline.config.executorSupplier.forLightweightBackgroundTasks()
      val uiThreadExecutor = uiThreadExecutor ?: UiThreadImmediateExecutorService.getInstance()
      initialize(
          DefaultFrescoVitoProvider(
              vitoConfig,
              imagePipeline,
              createImagePipelineUtils(useNativeCode),
              lightweightBackgroundThreadExecutor,
              uiThreadExecutor,
              callerContextVerifier,
              vitoImagePerfListener,
              debugOverlayEnabledSupplier?.let {
                DefaultDebugOverlayFactory2(
                    showExtendedDebugOverlayInformation,
                    showExtendedImageSourceExtraInformation,
                    it)
              } ?: NoOpDebugOverlayFactory2(),
              imagePerfListenerSupplier))
    }

    /**
     * Initialize Fresco Vito. Note: Fresco has to be initialized already, via
     * Fresco.initialize(...)
     *
     * @param providerImplementation the provider implementation to be used
     */
    @Synchronized
    fun initialize(providerImplementation: FrescoVitoSetup) {
      if (isInitialized) {
        return
      }
      FrescoVitoComponents.setImplementation(providerImplementation)
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

/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.provider.impl

import com.facebook.callercontext.CallerContextVerifier
import com.facebook.common.internal.Supplier
import com.facebook.fresco.vito.core.FrescoController2
import com.facebook.fresco.vito.core.FrescoVitoConfig
import com.facebook.fresco.vito.core.FrescoVitoPrefetcher
import com.facebook.fresco.vito.core.ImagePerfLoggingListener
import com.facebook.fresco.vito.core.ImagePipelineUtils
import com.facebook.fresco.vito.core.VitoImagePerfListener
import com.facebook.fresco.vito.core.VitoImagePipeline
import com.facebook.fresco.vito.core.impl.FrescoController2Impl
import com.facebook.fresco.vito.core.impl.FrescoVitoPrefetcherImpl
import com.facebook.fresco.vito.core.impl.HierarcherImpl
import com.facebook.fresco.vito.core.impl.VitoImagePipelineImpl
import com.facebook.fresco.vito.core.impl.debug.DebugOverlayFactory2
import com.facebook.fresco.vito.core.impl.debug.NoOpDebugOverlayFactory2
import com.facebook.fresco.vito.drawable.ArrayVitoDrawableFactory
import com.facebook.fresco.vito.drawable.BitmapDrawableFactory
import com.facebook.fresco.vito.draweesupport.DrawableFactoryWrapper
import com.facebook.fresco.vito.options.ImageOptionsDrawableFactory
import com.facebook.fresco.vito.provider.setup.FrescoVitoSetup
import com.facebook.imagepipeline.core.ImagePipeline
import com.facebook.imagepipeline.core.ImagePipelineFactory
import java.util.concurrent.Executor

class DefaultFrescoVitoProvider(
    private val frescoVitoConfig: FrescoVitoConfig,
    imagePipeline: ImagePipeline,
    imagePipelineUtils: ImagePipelineUtils,
    lightweightBackgroundThreadExecutor: Executor,
    uiThreadExecutor: Executor,
    callerContextVerifier: CallerContextVerifier,
    vitoImagePerfListener: VitoImagePerfListener,
    debugOverlayFactory: DebugOverlayFactory2 = NoOpDebugOverlayFactory2(),
    imagePerfListenerSupplier: Supplier<ImagePerfLoggingListener>? = null,
) : FrescoVitoSetup {

  private val frescoController: FrescoController2

  private val vitoImagePipeline: VitoImagePipeline

  private val frescoVitoPrefetcher: FrescoVitoPrefetcher

  init {
    if (!ImagePipelineFactory.hasBeenInitialized()) {
      throw RuntimeException(
          "Fresco must be initialized before DefaultFrescoVitoProvider can be used!")
    }
    frescoVitoPrefetcher =
        FrescoVitoPrefetcherImpl(imagePipeline, imagePipelineUtils, callerContextVerifier)
    vitoImagePipeline = VitoImagePipelineImpl(imagePipeline, imagePipelineUtils, frescoVitoConfig)
    frescoController =
        FrescoController2Impl(
            frescoVitoConfig,
            HierarcherImpl(createDefaultDrawableFactory()),
            lightweightBackgroundThreadExecutor,
            uiThreadExecutor,
            vitoImagePipeline,
            null,
            debugOverlayFactory,
            imagePerfListenerSupplier,
            vitoImagePerfListener)
  }

  override fun getController(): FrescoController2 = frescoController

  override fun getPrefetcher(): FrescoVitoPrefetcher = frescoVitoPrefetcher

  override fun getImagePipeline(): VitoImagePipeline = vitoImagePipeline

  override fun getConfig(): FrescoVitoConfig = frescoVitoConfig

  companion object {
    private fun createDefaultDrawableFactory(): ImageOptionsDrawableFactory {
      val animatedDrawableFactory =
          ImagePipelineFactory.getInstance()
              .getAnimatedDrawableFactory(null)
              ?.let(DrawableFactoryWrapper::wrap)
      val bitmapFactory = BitmapDrawableFactory()
      val xmlFactory =
          ImagePipelineFactory.getInstance().xmlDrawableFactory?.let(DrawableFactoryWrapper::wrap)
      val factories = listOfNotNull(bitmapFactory, animatedDrawableFactory, xmlFactory)
      return when (factories.size) {
        1 -> factories[0]
        else -> ArrayVitoDrawableFactory(*factories.toTypedArray())
      }
    }
  }
}

/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.ig.imageloader.sample

import android.app.Application
import android.util.Log
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.fresco.vito.init.FrescoVito
import com.facebook.quicklog.NoOpQuickPerformanceLogger
import com.facebook.quicklog.QuickPerformanceLoggerProvider
import com.instagram.common.api.base.Downloader
import com.instagram.common.cache.image.IgImageInfraProvider
import com.instagram.common.session.SampleSessionFactory
import com.instagram.common.ui.widget.imageview.IgImageView
import com.instagram.fresco.IgFrescoInitializer
import com.instagram.fresco.vitoutils.VitoExperimentHelper

/**
 * Initializes the image pipeline with toggle-based MobileConfig-style overrides.
 *
 * The 8 toggles mirror production MC flags in `MC.ig4a_fresco_pipeline_with_igidv2`:
 * - **use_vito**: Vito rendering vs legacy IgImageView rendering
 * - **use_fresco_pipeline**: Fresco's ImagePipeline vs IG's IgVitoImagePipeline
 * - **use_fresco_in_cache_request**: CacheRequest routes to Fresco vs IG's IgImageInfra
 * - **use_fresco_network_pipeline**: Fresco uses IG's UnifiedImageNetworkLayerAdapter
 * - **use_fresco_progressive_rendering**: Progressive JPEG rendering via streaming
 * - **use_fresco_bitmap_memory_cache**: IG pipeline uses Fresco's LruCountingMemoryCache
 * - **use_ig_cache_in_fresco**: IG pipeline uses the owner-based decoded-image cache path
 * - **enable_fresco_disk_cache**: IG pipeline uses SharedDiskCacheFactory
 *
 * All flags require app restart since they're baked into initialization.
 */
class SampleApplication : Application() {

  lateinit var toggleStates: Map<String, Boolean>
    private set

  override fun onCreate() {
    super.onCreate()

    toggleStates = SampleToggle.loadStates(this)
    Log.d(TAG, "Toggle states: $toggleStates")

    val useFrescoPipeline = toggleStates["use_fresco_pipeline"] ?: true
    val useFrescoInCacheRequest = toggleStates["use_fresco_in_cache_request"] ?: true
    val useFrescoMemoryCache = toggleStates["use_fresco_bitmap_memory_cache"] ?: false
    val useIgCacheInFresco = toggleStates["use_ig_cache_in_fresco"] ?: false
    val useFrescoDiskCache = toggleStates["enable_fresco_disk_cache"] ?: false

    // 1. Initialize session (real DeviceSession + UserSession)
    SampleSessionFactory.initialize(this)

    // 2. Initialize QPL (no-op logger — prevents NPEs)
    QuickPerformanceLoggerProvider.setQuickPerformanceLogger(NoOpQuickPerformanceLogger())

    // 3. Set custom Downloader for IG's network path (HttpURLConnection-based)
    Downloader.setInstance(SampleDownloader())

    // 4. Bootstrap IG storage chain (needed for disk cache toggles)
    SampleInfraFactory.bootstrapStorage(this)

    // 5. Build real IgImageInfra (always — needed for both Fresco and IG paths)
    val session = SampleSessionFactory.createUserSession()
    val memoryCache =
        SampleInfraFactory.createMemoryCache(
            this,
            useFrescoMemoryCache,
            useIgCacheInFresco,
        )
    val diskCacheFactory = SampleInfraFactory.createDiskCacheFactory(this, useFrescoDiskCache)
    SampleInfraFactory.buildIgImageInfra(this, session, memoryCache, diskCacheFactory)

    // 6. Build Fresco pipeline config (if Fresco pipeline toggle is ON)
    if (useFrescoPipeline) {
      val config =
          SampleInfraFactory.buildFrescoImagePipelineConfig(
              this,
              toggleStates,
              memoryCache,
              session,
          )
      Fresco.initialize(this, config)
    } else {
      Fresco.initialize(this)
    }

    // 7. Initialize IgFresco + Vito
    if (useFrescoPipeline) {
      FrescoVito.initialize()
    } else {
      IgFrescoInitializer.init()
    }

    // 8. Set CacheRequest routing flag
    IgImageInfraProvider.setUseFrescoPipelineInCacheRequest(
        useFrescoInCacheRequest && useFrescoPipeline
    )

    // 9. Set Vito rendering flag — construct with real constructor so all mc-dependent
    // fields are properly initialized, then override isVitoEnabledGlobally to respect
    // the use_vito toggle (mc defaults return false for all booleans)
    val useVito = toggleStates["use_vito"] ?: true
    val helper = VitoExperimentHelper(session)
    if (useVito) {
      try {
        val field = VitoExperimentHelper::class.java.getDeclaredField("isVitoEnabledGlobally")
        field.isAccessible = true
        field.set(helper, true)
      } catch (e: Exception) {
        Log.w(TAG, "Could not set isVitoEnabledGlobally", e)
      }
    }
    IgImageView.setVitoExperimentHelper(helper)

    Log.d(TAG, "Initialization complete — activeRoute: ${describeRoute()}")
  }

  fun describeRoute(): String {
    val useVito = toggleStates["use_vito"] ?: true
    val useFrescoPipeline = toggleStates["use_fresco_pipeline"] ?: true
    val useFrescoInCacheRequest = toggleStates["use_fresco_in_cache_request"] ?: true
    return when {
      useVito && useFrescoPipeline -> "Vito → Fresco Pipeline"
      useVito && !useFrescoPipeline -> "Vito → IG Pipeline"
      !useVito && useFrescoInCacheRequest && useFrescoPipeline -> "Legacy → CacheRequest → Fresco"
      else -> "Legacy → CacheRequest → IG Pipeline"
    }
  }

  companion object {
    private const val TAG = "IgImageLoaderSample"
  }
}

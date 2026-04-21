/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.ig.imageloader.sample

import android.content.Context

enum class ToggleCategory(val label: String) {
  RENDERING("Rendering"),
  PIPELINE("Pipeline"),
  CACHE("Cache (IG Pipeline)"),
}

data class SampleToggle(
    val key: String,
    val label: String,
    val description: String,
    val category: ToggleCategory,
    val defaultValue: Boolean,
) {
  companion object {
    private const val PREFS_NAME = "vito_sample_toggles"

    val ALL =
        listOf(
            SampleToggle(
                key = "use_vito",
                label = "Use Vito",
                description =
                    "ON: IgImageView → showWithVito(). OFF: legacy buildRequest() → CacheRequest.queue()",
                category = ToggleCategory.RENDERING,
                defaultValue = true,
            ),
            SampleToggle(
                key = "use_fresco_pipeline",
                label = "Use Fresco Pipeline",
                description =
                    "ON: Vito fetches via Fresco's ImagePipeline. OFF: Vito → IgVitoImagePipeline → IgImageInfra",
                category = ToggleCategory.PIPELINE,
                defaultValue = true,
            ),
            SampleToggle(
                key = "use_fresco_in_cache_request",
                label = "Use Fresco in CacheRequest",
                description =
                    "ON: CacheRequest.queue() → queueWithFresco(). OFF: → IgImageInfra.loadImage()",
                category = ToggleCategory.PIPELINE,
                defaultValue = true,
            ),
            SampleToggle(
                key = "use_fresco_network_pipeline",
                label = "Fresco Network Pipeline",
                description =
                    "ON: Fresco uses UnifiedImageNetworkLayerAdapter → IG HTTP layer. OFF: default HttpUrlConnection",
                category = ToggleCategory.PIPELINE,
                defaultValue = false,
            ),
            SampleToggle(
                key = "use_fresco_progressive_rendering",
                label = "Fresco Progressive Rendering",
                description =
                    "ON: Images render progressively as JPEG scans arrive (requires Network Pipeline ON). OFF: full image rendered after complete download",
                category = ToggleCategory.PIPELINE,
                defaultValue = false,
            ),
            SampleToggle(
                key = "use_fresco_bitmap_memory_cache",
                label = "Fresco Bitmap Memory Cache",
                description =
                    "ON: IG pipeline uses IgFrescoBitmapCacheAdapter wrapping Fresco's LruCountingMemoryCache. OFF: IG native cache",
                category = ToggleCategory.CACHE,
                defaultValue = false,
            ),
            SampleToggle(
                key = "use_ig_cache_in_fresco",
                label = "IG Cache In Fresco",
                description =
                    "ON: IG pipeline uses IgFrescoBitmapCacheAdapter wrapping the owner-based decoded-image cache. OFF: Approach 2 disabled",
                category = ToggleCategory.CACHE,
                defaultValue = false,
            ),
            SampleToggle(
                key = "enable_fresco_disk_cache",
                label = "Fresco Disk Cache",
                description =
                    "ON: SharedDiskCacheFactory (shared with Fresco). OFF: DiskCacheFactoryImpl (IG-only)",
                category = ToggleCategory.CACHE,
                defaultValue = false,
            ),
        )

    fun loadStates(context: Context): Map<String, Boolean> {
      val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      return ALL.associate { toggle ->
        toggle.key to prefs.getBoolean(toggle.key, toggle.defaultValue)
      }
    }

    fun save(context: Context, key: String, value: Boolean) {
      context
          .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
          .edit()
          .putBoolean(key, value)
          .apply()
    }
  }
}

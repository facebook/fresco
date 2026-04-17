/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.vito

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment
import com.facebook.fresco.samples.showcase.R
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.fresco.vito.options.RoundingOptions
import com.facebook.fresco.vito.provider.FrescoVitoProvider
import com.facebook.fresco.vito.source.ImageSourceProvider
import com.facebook.fresco.vito.view.VitoView

class VitoViewPrefetchFragment : BaseShowcaseFragment() {

  private val imageOptions =
      ImageOptions.create()
          .round(RoundingOptions.asCircle())
          .placeholderRes(R.color.placeholder_color)
          .build()

  private var imageVisible = false

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?,
  ): View {
    return inflater.inflate(R.layout.fragment_vito_view_prefetch, container, false)
  }

  override fun onViewCreated(container: View, savedInstanceState: Bundle?) {
    val uri = sampleUris().createSampleUri(ImageUriProvider.ImageSize.M)
    val view = container.findViewById<View>(R.id.view)

    val prefetcher = FrescoVitoProvider.getPrefetcher()

    val buttonPrefetchBitmap = container.findViewById<Button>(R.id.button_prefetch_bitmap)
    val buttonPrefetchEncoded = container.findViewById<Button>(R.id.button_prefetch_encoded)
    val buttonPrefetchDisk = container.findViewById<Button>(R.id.button_prefetch_disk)
    val buttonToggleImages = container.findViewById<Button>(R.id.button_toggle_images)
    val buttonClearCache = container.findViewById<Button>(R.id.button_clear_cache)
    val buttonClearCacheSingleItem =
        container.findViewById<Button>(R.id.button_clear_cache_single_item)

    buttonPrefetchBitmap.setOnClickListener {
      prefetcher.prefetchToBitmapCache(uri, imageOptions, "prefetch_bitmap", "sample")
    }

    buttonPrefetchEncoded.setOnClickListener {
      prefetcher.prefetchToEncodedCache(uri, imageOptions, "prefetch_encoded", "sample")
    }

    buttonPrefetchDisk.setOnClickListener {
      prefetcher.prefetchToDiskCache(uri, imageOptions, "prefetch_disk", "sample")
    }

    buttonToggleImages.setOnClickListener {
      if (imageVisible) {
        VitoView.show(null, view)
      } else {
        VitoView.show(uri, imageOptions, view)
      }
      imageVisible = !imageVisible
    }

    buttonClearCache.setOnClickListener { Fresco.getImagePipeline().clearCaches() }
    buttonClearCacheSingleItem.setOnClickListener {
      FrescoVitoProvider.getImagePipeline()
          .evictFromCaches(
              FrescoVitoProvider.getImagePipeline()
                  .createImageRequest(resources, ImageSourceProvider.forUri(uri), imageOptions)
          )
    }
  }
}

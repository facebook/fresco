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
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment
import com.facebook.fresco.samples.showcase.R
import com.facebook.fresco.samples.showcase.databinding.FragmentVitoViewPrefetchBinding
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.fresco.vito.options.RoundingOptions
import com.facebook.fresco.vito.provider.FrescoVitoProvider
import com.facebook.fresco.vito.source.ImageSourceProvider
import com.facebook.fresco.vito.view.VitoView

class VitoViewPrefetchFragment : BaseShowcaseFragment() {

  private var _binding: FragmentVitoViewPrefetchBinding? = null
  private val binding
    get() = _binding!!

  private val buttonPrefetchBitmap
    get() = binding.buttonPrefetchBitmap

  private val buttonPrefetchEncoded
    get() = binding.buttonPrefetchEncoded

  private val buttonPrefetchDisk
    get() = binding.buttonPrefetchDisk

  private val buttonToggleImages
    get() = binding.buttonToggleImages

  private val buttonClearCache
    get() = binding.buttonClearCache

  private val buttonClearCacheSingleItem
    get() = binding.buttonClearCacheSingleItem

  private val imageOptions =
      ImageOptions.create()
          .round(RoundingOptions.asCircle())
          .placeholderRes(R.color.placeholder_color)
          .build()

  private var imageVisible = false

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    _binding = FragmentVitoViewPrefetchBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(container: View, savedInstanceState: Bundle?) {
    val uri = sampleUris().createSampleUri(ImageUriProvider.ImageSize.M)
    val view = container.findViewById<View>(R.id.view)

    val prefetcher = FrescoVitoProvider.getPrefetcher()

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
                  .createImageRequest(resources, ImageSourceProvider.forUri(uri), imageOptions))
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}

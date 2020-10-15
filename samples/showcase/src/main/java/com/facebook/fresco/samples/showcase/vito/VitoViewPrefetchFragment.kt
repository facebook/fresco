/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.vito

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment
import com.facebook.fresco.samples.showcase.R
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.fresco.vito.options.RoundingOptions
import com.facebook.fresco.vito.provider.FrescoVitoProvider
import com.facebook.fresco.vito.view.VitoView
import kotlinx.android.synthetic.main.fragment_vito_view_prefetch.*

class VitoViewPrefetchFragment : BaseShowcaseFragment() {

    private val imageOptions = ImageOptions.create()
            .round(RoundingOptions.asCircle())
            .placeholderRes(R.color.placeholder_color)
            .build()

    private var imageVisible = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_vito_view_prefetch, container, false)
    }

    override fun onViewCreated(container: View, savedInstanceState: Bundle?) {
        val uri = sampleUris().createSampleUri(ImageUriProvider.ImageSize.M)
        val view = container.findViewById<View>(R.id.view)

        val prefetcher = FrescoVitoProvider.getPrefetcher()

        button_prefetch_bitmap.setOnClickListener {
            prefetcher.prefetchToBitmapCache(uri, imageOptions, "prefetch_bitmap")
        }

        button_prefetch_encoded.setOnClickListener {
            prefetcher.prefetchToEncodedCache(uri, imageOptions, "prefetch_encoded")
        }

        button_prefetch_disk.setOnClickListener {
            prefetcher.prefetchToDiskCache(uri, imageOptions, "prefetch_disk")
        }

        button_toggle_images.setOnClickListener {
            if (imageVisible) {
                VitoView.show(null as? Uri, view)
            } else {
                VitoView.show(uri, imageOptions, view)
            }
            imageVisible = !imageVisible
        }

        button_clear_cache.setOnClickListener {
            Fresco.getImagePipeline().clearCaches()
        }
    }

    override fun getTitleId(): Int = R.string.vito_view_prefetch
}

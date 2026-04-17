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
import android.widget.ImageView
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment
import com.facebook.fresco.samples.showcase.R
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider
import com.facebook.fresco.vito.ktx.ImageSourceExtensions.asImageSource
import com.facebook.fresco.vito.source.ImageSourceProvider
import com.facebook.fresco.vito.view.VitoView

/**
 * Fragment that displays an image based on a set of image requests, either with increasing quality
 * or the first available.
 */
class MultiUriFragment : BaseShowcaseFragment() {

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?,
  ): View {
    return inflater.inflate(R.layout.fragment_vito_multi_uri, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    val imageView = view.findViewById<ImageView>(R.id.image_view)
    val btnFirstAvailable = view.findViewById<Button>(R.id.btn_first_available)
    val btnIncreasingQuality = view.findViewById<Button>(R.id.btn_increasing_quality)
    val btnBoth = view.findViewById<Button>(R.id.btn_both)

    btnFirstAvailable.setOnClickListener {
      val requests =
          sampleUris().createSampleUriSet().map(ImageSourceProvider::forUri).toTypedArray()
      VitoView.show(ImageSourceProvider.firstAvailable(*requests), imageView)
    }

    btnIncreasingQuality.setOnClickListener {
      val lowRes = sampleUris().createSampleUri(ImageUriProvider.ImageSize.XS).asImageSource()
      val highRes = sampleUris().createSampleUri(ImageUriProvider.ImageSize.XXL).asImageSource()
      VitoView.show(ImageSourceProvider.increasingQuality(lowRes, highRes), imageView)
    }

    btnBoth.setOnClickListener {
      val lowRes = sampleUris().createSampleUri(ImageUriProvider.ImageSize.XS).asImageSource()
      val anyHighRes =
          listOf(
                  sampleUris().createSampleUri(ImageUriProvider.ImageSize.L),
                  sampleUris().createSampleUri(ImageUriProvider.ImageSize.XL),
                  sampleUris().createSampleUri(ImageUriProvider.ImageSize.XXL),
              )
              .map(ImageSourceProvider::forUri)
              .toTypedArray()
      VitoView.show(
          ImageSourceProvider.increasingQuality(
              lowRes,
              ImageSourceProvider.firstAvailable(*anyHighRes),
          ),
          imageView,
      )
    }
  }
}

/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.drawee

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment
import com.facebook.fresco.samples.showcase.databinding.FragmentDraweeMultiUriBinding
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider
import com.facebook.fresco.vito.ktx.ImageSourceExtensions.asImageSource
import com.facebook.fresco.vito.source.ImageSourceProvider
import com.facebook.fresco.vito.view.VitoView
import com.facebook.imagepipeline.request.ImageRequest
import com.google.android.material.switchmaterial.SwitchMaterial

/**
 * Fragment that displays an image based on a set of image requests, either with increasing quality
 * or the first available.
 */
class MultiUriFragment : BaseShowcaseFragment() {

  private var _binding: FragmentDraweeMultiUriBinding? = null
  private val binding
    get() = _binding!!

  private val draweeView: SimpleDraweeView
    get() = binding.draweeView

  private val switchUseVito: SwitchMaterial
    get() = binding.switchUseVito

  private val btnFirstAvailable: Button
    get() = binding.btnFirstAvailable

  private val btnIncreasingQuality
    get() = binding.btnIncreasingQuality

  private val btnBoth
    get() = binding.btnBoth

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    _binding = FragmentDraweeMultiUriBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    btnFirstAvailable.setOnClickListener {
      resetImageView()
      if (switchUseVito.isChecked) {
        val requests =
            sampleUris().createSampleUriSet().map(ImageSourceProvider::forUri).toTypedArray()
        VitoView.show(ImageSourceProvider.firstAvailable(*requests), draweeView)
      } else {
        val requests = sampleUris().createSampleUriSet().map(ImageRequest::fromUri).toTypedArray()
        draweeView.controller =
            Fresco.newDraweeControllerBuilder().setFirstAvailableImageRequests(requests).build()
      }
    }

    btnIncreasingQuality.setOnClickListener {
      resetImageView()

      if (switchUseVito.isChecked) {
        val lowRes = sampleUris().createSampleUri(ImageUriProvider.ImageSize.XS).asImageSource()
        val highRes = sampleUris().createSampleUri(ImageUriProvider.ImageSize.XXL).asImageSource()
        VitoView.show(ImageSourceProvider.increasingQuality(lowRes, highRes), draweeView)
      } else {
        val lowRes =
            ImageRequest.fromUri(sampleUris().createSampleUri(ImageUriProvider.ImageSize.XS))
        val highRes =
            ImageRequest.fromUri(sampleUris().createSampleUri(ImageUriProvider.ImageSize.XXL))
        val draweeControllerBuilder = Fresco.newDraweeControllerBuilder().setImageRequest(highRes)
        lowRes?.let { draweeControllerBuilder.setLowResImageRequest(it) }
        draweeView.controller = draweeControllerBuilder.build()
      }
    }

    btnBoth.setOnClickListener {
      resetImageView()
      if (switchUseVito.isChecked) {
        val lowRes = sampleUris().createSampleUri(ImageUriProvider.ImageSize.XS).asImageSource()
        val anyHighRes =
            listOf(
                    sampleUris().createSampleUri(ImageUriProvider.ImageSize.L),
                    sampleUris().createSampleUri(ImageUriProvider.ImageSize.XL),
                    sampleUris().createSampleUri(ImageUriProvider.ImageSize.XXL))
                .map(ImageSourceProvider::forUri)
                .toTypedArray()
        VitoView.show(
            ImageSourceProvider.increasingQuality(
                lowRes, ImageSourceProvider.firstAvailable(*anyHighRes)),
            draweeView)
      } else {
        val lowRes =
            ImageRequest.fromUri(sampleUris().createSampleUri(ImageUriProvider.ImageSize.XS))
        val anyHighRes =
            listOf(
                    sampleUris().createSampleUri(ImageUriProvider.ImageSize.L),
                    sampleUris().createSampleUri(ImageUriProvider.ImageSize.XL),
                    sampleUris().createSampleUri(ImageUriProvider.ImageSize.XXL))
                .map(ImageRequest::fromUri)
                .toTypedArray()
        val draweeControllerBuilder =
            Fresco.newDraweeControllerBuilder().setFirstAvailableImageRequests(anyHighRes)
        lowRes?.let { draweeControllerBuilder.setLowResImageRequest(it) }
        draweeView.controller = draweeControllerBuilder.build()
      }
    }
  }

  private fun resetImageView() {
    draweeView.controller = null
    VitoView.show(null, draweeView)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}

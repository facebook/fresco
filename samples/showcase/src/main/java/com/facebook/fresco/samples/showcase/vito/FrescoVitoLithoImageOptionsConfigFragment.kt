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
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment
import com.facebook.fresco.samples.showcase.R
import com.facebook.fresco.samples.showcase.common.SpinnerUtils.setupWithList
import com.facebook.fresco.samples.showcase.databinding.FragmentVitoImageOptionsConfigBinding
import com.facebook.fresco.samples.showcase.misc.DebugImageListener
import com.facebook.fresco.vito.litho.FrescoVitoImage2
import com.facebook.fresco.vito.litho.FrescoVitoTapToRetryImage
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.litho.ComponentContext
import com.facebook.litho.LithoView

/** Experimental Fresco Vito fragment that allows to configure ImageOptions via a simple UI. */
class FrescoVitoLithoImageOptionsConfigFragment : BaseShowcaseFragment() {

  private var _binding: FragmentVitoImageOptionsConfigBinding? = null
  private val binding
    get() = _binding!!

  private val container
    get() = binding.container

  private val spinnerRounding
    get() = binding.spinnerRounding

  private val spinnerBorder
    get() = binding.spinnerBorder

  private val spinnerScaleType
    get() = binding.spinnerScaleType

  private val spinnerImageSource
    get() = binding.spinnerImageSource

  private val spinnerImageFormat
    get() = binding.spinnerImageFormat

  private val spinnerColorFilter
    get() = binding.spinnerColorFilter

  private val spinnerPlaceholder
    get() = binding.spinnerPlaceholder

  private val spinnerError
    get() = binding.spinnerError

  private val spinnerOverlay
    get() = binding.spinnerOverlay

  private val spinnerFading
    get() = binding.spinnerFading

  private val spinnerProgress
    get() = binding.spinnerProgress

  private val spinnerPostprocessor
    get() = binding.spinnerPostprocessor

  private val spinnerRotation
    get() = binding.spinnerRotation

  private val spinnerResize
    get() = binding.spinnerResize

  private val spinnerCustomDrawableFactory
    get() = binding.spinnerCustomDrawableFactory

  private val switchAutoPlayAnimations
    get() = binding.switchAutoPlayAnimations

  private val imageListener = DebugImageListener()
  private val imageOptionsBuilder = ImageOptions.create().placeholderApplyRoundingOptions(true)
  private val imageSourceProvider = ImageSourceConfigurator(sampleUris())

  private var componentContext: ComponentContext? = null
  private var lithoView: LithoView? = null
  private val useTapToRetry = false

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?,
  ): View {
    _binding = FragmentVitoImageOptionsConfigBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    componentContext = ComponentContext(requireContext())

    lithoView = LithoView.create(componentContext, createImage(imageOptionsBuilder.build()))
    container.addView(lithoView)

    container.setOnClickListener { refresh() }

    spinnerRounding.setupWithList(VitoSpinners.roundingOptions) {
      refresh(imageOptionsBuilder.round(it))
    }
    spinnerBorder.setupWithList(VitoSpinners.borderOptions) {
      refresh(imageOptionsBuilder.borders(it))
    }
    spinnerScaleType.setupWithList(VitoSpinners.scaleTypes) {
      refresh(imageOptionsBuilder.scale(it.first).focusPoint(it.second))
    }
    spinnerImageSource.setupWithList(imageSourceProvider.imageSources) {
      it()
      refresh()
    }
    spinnerImageFormat.setupWithList(imageSourceProvider.imageFormatUpdater) {
      it()
      refresh()
    }
    spinnerColorFilter.setupWithList(VitoSpinners.colorFilters) {
      refresh(imageOptionsBuilder.colorFilter(it))
    }
    spinnerPlaceholder.setupWithList(VitoSpinners.placeholderOptions(resources)) {
      refresh(it(imageOptionsBuilder))
    }
    spinnerError.setupWithList(VitoSpinners.errorOptions) { refresh(it(imageOptionsBuilder)) }
    spinnerOverlay.setupWithList(VitoSpinners.overlayOptions) { refresh(it(imageOptionsBuilder)) }
    spinnerFading.setupWithList(VitoSpinners.fadingOptions) {
      refresh(imageOptionsBuilder.fadeDurationMs(it))
    }
    spinnerProgress.setupWithList(VitoSpinners.progressOptions) {
      refresh(it(requireContext(), imageOptionsBuilder))
    }
    spinnerPostprocessor.setupWithList(VitoSpinners.postprocessorOptions) {
      refresh(it(imageOptionsBuilder))
    }
    spinnerRotation.setupWithList(VitoSpinners.rotationOptions) {
      refresh(imageOptionsBuilder.rotate(it))
    }
    spinnerResize.setupWithList(VitoSpinners.resizeOptions) { refresh(it(imageOptionsBuilder)) }
    spinnerCustomDrawableFactory.setupWithList(VitoSpinners.customDrawableFactoryOptions) {
      refresh(it(imageOptionsBuilder))
    }
    switchAutoPlayAnimations.setOnCheckedChangeListener { _, isChecked ->
      refresh(imageOptionsBuilder.autoPlay(isChecked))
    }
    imageOptionsBuilder.autoPlay(switchAutoPlayAnimations.isChecked)
  }

  private fun refresh(builder: ImageOptions.Builder = imageOptionsBuilder) {
    lithoView?.setComponentAsync(createImage(builder.build()))
  }

  private fun createImage(imageOptions: ImageOptions) =
      if (!useTapToRetry) {
        FrescoVitoImage2.create(componentContext)
            .imageSource(imageSourceProvider.imageSource)
            .imageOptions(imageOptions)
            .imageListener(imageListener)
            .build()
      } else {
        FrescoVitoTapToRetryImage.create(componentContext)
            .imageSource(imageSourceProvider.imageSource)
            .imageOptions(imageOptions)
            .retryImageRes(R.drawable.ic_retry_black_48dp)
            .maxTapCount(5)
            .imageListener(imageListener)
            .build()
      }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}

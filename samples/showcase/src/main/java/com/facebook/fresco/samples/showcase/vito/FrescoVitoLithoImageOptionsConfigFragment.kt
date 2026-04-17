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
import android.widget.FrameLayout
import android.widget.Spinner
import android.widget.Switch
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment
import com.facebook.fresco.samples.showcase.R
import com.facebook.fresco.samples.showcase.common.SpinnerUtils.setupWithList
import com.facebook.fresco.samples.showcase.misc.DebugImageListener
import com.facebook.fresco.vito.litho.FrescoVitoImage2
import com.facebook.fresco.vito.litho.FrescoVitoTapToRetryImage
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.litho.ComponentContext
import com.facebook.litho.LithoView

/** Experimental Fresco Vito fragment that allows to configure ImageOptions via a simple UI. */
class FrescoVitoLithoImageOptionsConfigFragment : BaseShowcaseFragment() {

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
    return inflater.inflate(R.layout.fragment_vito_image_options_config, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    componentContext = ComponentContext(requireContext())

    val container = view.findViewById<FrameLayout>(R.id.container)
    val spinnerRounding = view.findViewById<Spinner>(R.id.spinner_rounding)
    val spinnerBorder = view.findViewById<Spinner>(R.id.spinner_border)
    val spinnerScaleType = view.findViewById<Spinner>(R.id.spinner_scale_type)
    val spinnerImageSource = view.findViewById<Spinner>(R.id.spinner_image_source)
    val spinnerImageFormat = view.findViewById<Spinner>(R.id.spinner_image_format)
    val spinnerColorFilter = view.findViewById<Spinner>(R.id.spinner_color_filter)
    val spinnerPlaceholder = view.findViewById<Spinner>(R.id.spinner_placeholder)
    val spinnerError = view.findViewById<Spinner>(R.id.spinner_error)
    val spinnerOverlay = view.findViewById<Spinner>(R.id.spinner_overlay)
    val spinnerFading = view.findViewById<Spinner>(R.id.spinner_fading)
    val spinnerProgress = view.findViewById<Spinner>(R.id.spinner_progress)
    val spinnerPostprocessor = view.findViewById<Spinner>(R.id.spinner_postprocessor)
    val spinnerRotation = view.findViewById<Spinner>(R.id.spinner_rotation)
    val spinnerResize = view.findViewById<Spinner>(R.id.spinner_resize)
    val spinnerCustomDrawableFactory =
        view.findViewById<Spinner>(R.id.spinner_custom_drawable_factory)
    val switchAutoPlayAnimations = view.findViewById<Switch>(R.id.switch_auto_play_animations)

    lithoView = LithoView.create(componentContext!!, createImage(imageOptionsBuilder.build()))
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
    spinnerImageSource.setupWithList(imageSourceProvider.imageSources) { imageSourceSetter ->
      imageSourceSetter()
      refresh()
    }
    spinnerImageFormat.setupWithList(imageSourceProvider.imageFormatUpdater) { formatUpdater ->
      formatUpdater()
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
}

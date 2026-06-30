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
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.litho.ComponentContext
import com.facebook.litho.LithoView

/** Experimental Fresco Vito fragment that allows to configure ImageOptions via a simple UI. */
class FrescoVitoLithoImageOptionsConfigFragment : BaseShowcaseFragment() {

  private val imageListener = DebugImageListener()
  private val imageOptionsBuilder = ImageOptions.create().placeholderApplyRoundingOptions(true)
  private val imageSourceProvider = ImageSourceConfigurator(sampleUris())

  private lateinit var componentContext: ComponentContext
  private var lithoView: LithoView? = null

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

    lithoView = LithoView.create(componentContext!!, createImage(imageOptionsBuilder.build()))
    container.addView(lithoView)

    container.setOnClickListener { refresh() }

    view.findViewById<Spinner>(R.id.spinner_rounding).setupWithList(VitoSpinners.roundingOptions) {
      refresh(imageOptionsBuilder.round(it))
    }
    view.findViewById<Spinner>(R.id.spinner_border).setupWithList(VitoSpinners.borderOptions) {
      refresh(imageOptionsBuilder.borders(it))
    }
    view.findViewById<Spinner>(R.id.spinner_scale_type).setupWithList(VitoSpinners.scaleTypes) {
      refresh(imageOptionsBuilder.scale(it.first).focusPoint(it.second))
    }
    view.findViewById<Spinner>(R.id.spinner_image_source).setupWithList(
        imageSourceProvider.imageSources
    ) { imageSourceSetter ->
      imageSourceSetter()
      refresh()
    }
    view.findViewById<Spinner>(R.id.spinner_image_format).setupWithList(
        imageSourceProvider.imageFormatUpdater
    ) { formatUpdater ->
      formatUpdater()
      refresh()
    }
    view.findViewById<Spinner>(R.id.spinner_color_filter).setupWithList(VitoSpinners.colorFilters) {
      refresh(imageOptionsBuilder.colorFilter(it))
    }
    view.findViewById<Spinner>(R.id.spinner_placeholder).setupWithList(
        VitoSpinners.placeholderOptions(resources)
    ) {
      refresh(it(imageOptionsBuilder))
    }
    view.findViewById<Spinner>(R.id.spinner_error).setupWithList(VitoSpinners.errorOptions) {
      refresh(it(imageOptionsBuilder))
    }
    view.findViewById<Spinner>(R.id.spinner_overlay).setupWithList(VitoSpinners.overlayOptions) {
      refresh(it(imageOptionsBuilder))
    }
    view.findViewById<Spinner>(R.id.spinner_fading).setupWithList(VitoSpinners.fadingOptions) {
      refresh(imageOptionsBuilder.fadeDurationMs(it))
    }
    view.findViewById<Spinner>(R.id.spinner_progress).setupWithList(VitoSpinners.progressOptions) {
      refresh(it(requireContext(), imageOptionsBuilder))
    }
    view.findViewById<Spinner>(R.id.spinner_postprocessor).setupWithList(
        VitoSpinners.postprocessorOptions
    ) {
      refresh(it(imageOptionsBuilder))
    }
    view.findViewById<Spinner>(R.id.spinner_rotation).setupWithList(VitoSpinners.rotationOptions) {
      refresh(imageOptionsBuilder.rotate(it))
    }
    view.findViewById<Spinner>(R.id.spinner_resize).setupWithList(VitoSpinners.resizeOptions) {
      refresh(it(imageOptionsBuilder))
    }
    view.findViewById<Spinner>(R.id.spinner_custom_drawable_factory).setupWithList(
        VitoSpinners.customDrawableFactoryOptions
    ) {
      refresh(it(imageOptionsBuilder))
    }
    val switchAutoPlay = view.findViewById<Switch>(R.id.switch_auto_play_animations)
    switchAutoPlay.setOnCheckedChangeListener { _, isChecked ->
      refresh(imageOptionsBuilder.autoPlay(isChecked))
    }
    imageOptionsBuilder.autoPlay(switchAutoPlay.isChecked)
  }

  private fun refresh(builder: ImageOptions.Builder = imageOptionsBuilder) {
    lithoView?.setComponentAsync(createImage(builder.build()))
  }

  private fun createImage(imageOptions: ImageOptions) =
      FrescoVitoImage2.create(componentContext)
          .imageSource(imageSourceProvider.imageSource)
          .imageOptions(imageOptions)
          .imageListener(imageListener)
          .build()
}

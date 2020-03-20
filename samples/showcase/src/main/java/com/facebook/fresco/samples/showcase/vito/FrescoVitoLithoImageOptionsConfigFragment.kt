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
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment
import com.facebook.fresco.samples.showcase.R
import com.facebook.fresco.samples.showcase.common.SpinnerUtils.setupWithList
import com.facebook.fresco.samples.showcase.misc.DebugImageListener
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider
import com.facebook.fresco.vito.litho.FrescoVitoImage2
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.litho.ComponentContext
import com.facebook.litho.LithoView
import kotlinx.android.synthetic.main.fragment_vito_image_options_config.*

/** Experimental Fresco Vito fragment that allows to configure ImageOptions via a simple UI.  */
class FrescoVitoLithoImageOptionsConfigFragment : BaseShowcaseFragment() {

    private val imageListener = DebugImageListener()
    private val imageOptionsBuilder = ImageOptions.create().autoPlay(true).placeholderApplyRoundingOptions(true)

    private var currentUri: Uri? = null
    private var componentContext: ComponentContext? = null
    private var lithoView: LithoView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_vito_image_options_config, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        currentUri = sampleUris().createSampleUri(ImageUriProvider.ImageSize.M)
        componentContext = ComponentContext(context)

        lithoView = LithoView.create(componentContext, createImage(imageOptionsBuilder.build(), currentUri))
        container.addView(lithoView)

        spinner_rounding.setupWithList(VitoSpinners.roundingOptions) {
            refresh(imageOptionsBuilder.round(it))
        }
        spinner_border.setupWithList(VitoSpinners.borderOptions) {
            refresh(imageOptionsBuilder.borders(it))
        }
        spinner_scale_type.setupWithList(VitoSpinners.scaleTypes) {
            refresh(imageOptionsBuilder.scale(it.first).focusPoint(it.second))
        }
        spinner_image_format.setupWithList(VitoSpinners.imageFormats) {
            refresh(uri = if (it != null) sampleUris().create(it) else null)
        }

        spinner_color_filter.setupWithList(VitoSpinners.colorFilters) {
            refresh(imageOptionsBuilder.colorFilter(it))
        }

        spinner_placeholder.setupWithList(VitoSpinners.placeholderOptions) {
            refresh(it(imageOptionsBuilder))
        }
        spinner_progress.setupWithList(VitoSpinners.progressOptions) {
            refresh(it(context!!, imageOptionsBuilder))
        }
        spinner_postprocessor.setupWithList(VitoSpinners.postprocessorOptions) {
            refresh(it(imageOptionsBuilder))
        }
        spinner_rotation.setupWithList(VitoSpinners.rotationOptions) {
            refresh(imageOptionsBuilder.rotate(it))
        }
    }

    override fun getTitleId() = R.string.vito_litho_image_options_config

    private fun refresh(builder: ImageOptions.Builder = imageOptionsBuilder, uri: Uri? = currentUri) {
        currentUri = uri
        lithoView?.setComponentAsync(createImage(builder.build(), uri))
    }

    private fun createImage(imageOptions: ImageOptions, uri: Uri?) = FrescoVitoImage2.create(componentContext)
            .uri(uri)
            .imageOptions(imageOptions)
            .imageListener(imageListener)
            .build()
}

/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.vito

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.View
import android.widget.TextView
import com.facebook.fresco.samples.showcase.BaseShowcaseKotlinFragment
import com.facebook.fresco.samples.showcase.R
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.fresco.vito.options.RoundingOptions
import com.facebook.fresco.vito.source.ImageSourceProvider
import com.facebook.fresco.vito.textspan.VitoSpan
import com.facebook.fresco.vito.textspan.VitoSpanLoader
import com.facebook.fresco.vito.textspan.VitoSpanLoader.setImageSpan

class VitoSpanFragment : BaseShowcaseKotlinFragment(R.layout.fragment_vito_text_span) {

  private val imageOptions =
      ImageOptions.create()
          .round(RoundingOptions.asCircle())
          .placeholderRes(R.drawable.logo)
          .autoPlay(true)
          .build()

  private var imageSpan: VitoSpan? = null

  override fun onViewCreated(container: View, savedInstanceState: Bundle?) {
    val uri = sampleUris().createGifUri(ImageUriProvider.ImageSize.S)
    val textView = container.findViewById<TextView>(R.id.text_view)
    val span: VitoSpan = VitoSpanLoader.createSpan(resources)
    val text = "Text with [] inline image"
    textView.text =
        SpannableStringBuilder(text).apply {
          setImageSpan(span, text.indexOf('['), text.indexOf(']'), 100, 100, textView)
        }

    VitoSpanLoader.show(
        ImageSourceProvider.forUri(uri), imageOptions, false, null, null, null, span)

    imageSpan = span
  }

  override fun onDestroyView() {
    super.onDestroyView()
    VitoSpanLoader.release(imageSpan)
  }
}

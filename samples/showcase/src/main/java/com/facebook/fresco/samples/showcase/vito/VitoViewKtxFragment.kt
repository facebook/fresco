/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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
import com.facebook.fresco.vito.ktx.ViewExtensions.show
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.fresco.vito.options.RoundingOptions
import kotlinx.android.synthetic.main.fragment_vito_view_ktx.*

class VitoViewKtxFragment : BaseShowcaseFragment() {

    private val imageOptions = ImageOptions.create()
            .round(RoundingOptions.asCircle())
            .placeholderRes(R.color.placeholder_color)
            .build()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_vito_view_ktx, container, false)
    }

    override fun onViewCreated(container: View, savedInstanceState: Bundle?) {
        val uri = sampleUris().createSampleUri()
        image?.show(uri, imageOptions)
    }

    override fun getTitleId() = R.string.vito_view_ktx
}

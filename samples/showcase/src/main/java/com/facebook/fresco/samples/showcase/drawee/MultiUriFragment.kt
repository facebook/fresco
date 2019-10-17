/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.drawee

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment
import com.facebook.fresco.samples.showcase.R
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider
import com.facebook.fresco.vito.view.VitoView
import com.facebook.imagepipeline.multiuri.MultiUri
import com.facebook.imagepipeline.request.ImageRequest
import kotlinx.android.synthetic.main.fragment_drawee_multi_uri.*

/**
 * Fragment that displays an image based on a set of image requests,
 * either with increasing quality or the first available.
 */
class MultiUriFragment : BaseShowcaseFragment() {

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_drawee_multi_uri, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        btn_first_available.setOnClickListener {
            resetImageView()
            val requests = sampleUris().createSampleUriSet().map(ImageRequest::fromUri).toTypedArray()
            if (switch_use_vito.isChecked) {
                VitoView.show(MultiUri.create().setImageRequests(*requests).build(), drawee_view)
            } else {
                drawee_view.setController(
                        Fresco.newDraweeControllerBuilder()
                                .setFirstAvailableImageRequests(requests)
                                .build())
            }
        }

        btn_increasing_quality.setOnClickListener {
            resetImageView()
            val lowRes = ImageRequest.fromUri(sampleUris().createSampleUri(ImageUriProvider.ImageSize.XS))
            val highRes = ImageRequest.fromUri(sampleUris().createSampleUri(ImageUriProvider.ImageSize.XXL))
            if (switch_use_vito.isChecked) {
                VitoView.show(MultiUri.create().setImageRequests(highRes).setLowResImageRequest(lowRes).build(), drawee_view)
            } else {
                drawee_view.setController(
                        Fresco.newDraweeControllerBuilder()
                                .setLowResImageRequest(lowRes)
                                .setImageRequest(highRes)
                                .build()
                )
            }
        }

        btn_both.setOnClickListener {
            resetImageView()
            val lowRes = ImageRequest.fromUri(sampleUris().createSampleUri(ImageUriProvider.ImageSize.XS))
            val anyHighRes = listOf(
                    sampleUris().createSampleUri(ImageUriProvider.ImageSize.L),
                    sampleUris().createSampleUri(ImageUriProvider.ImageSize.XL),
                    sampleUris().createSampleUri(ImageUriProvider.ImageSize.XXL))
                    .map(ImageRequest::fromUri).toTypedArray()
            if (switch_use_vito.isChecked) {
                VitoView.show(MultiUri.create().setImageRequests(*anyHighRes).setLowResImageRequest(lowRes).build(), drawee_view)
            } else {
                drawee_view.setController(
                        Fresco.newDraweeControllerBuilder()
                                .setLowResImageRequest(lowRes)
                                .setFirstAvailableImageRequests(anyHighRes)
                                .build()
                )
            }
        }
    }

    override fun getTitleId() = R.string.drawee_multi_uri_title

    private fun resetImageView() {
        drawee_view.controller = null
        VitoView.show(null as? Uri, drawee_view)
    }
}

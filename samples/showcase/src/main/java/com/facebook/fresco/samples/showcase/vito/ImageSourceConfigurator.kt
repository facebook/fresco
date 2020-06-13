/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.vito

import com.facebook.fresco.samples.showcase.imageformat.keyframes.KeyframesDecoderExample
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider
import com.facebook.fresco.vito.source.*
import com.facebook.imageformat.DefaultImageFormats
import com.facebook.imageformat.ImageFormat
import com.facebook.imagepipeline.request.ImageRequest

data class ImageSourceConfigurator(
        private val imageUriProvider: ImageUriProvider,
        var imageSource: ImageSource? = null,
        private var activeImageSourceProvider: () -> Unit = {},
        private var currentImageFormat: ImageFormat = DefaultImageFormats.JPEG) {

    private val imageFormats = listOf(
            "JPEG" to DefaultImageFormats.JPEG,
            "PNG" to DefaultImageFormats.PNG,
            "Animated GIF" to DefaultImageFormats.GIF,
            "WebP simple" to DefaultImageFormats.WEBP_SIMPLE,
            "WebP with alpha" to DefaultImageFormats.WEBP_EXTENDED_WITH_ALPHA,
            "Animated WebP" to DefaultImageFormats.WEBP_ANIMATED,
            "Keyframes" to KeyframesDecoderExample.IMAGE_FORMAT_KEYFRAMES
    )

    val imageFormatUpdater = Pair(
            imageFormats.map {
                Pair(it.first, {
                    currentImageFormat = it.second
                    activeImageSourceProvider()
                })
            }, "Image format")

    val imageSources = Pair(listOf(
            "Single URI" to { set { ImageSourceProvider.forUri(imageUriProvider.create(currentImageFormat)) } },
            "Single URI String" to {
                set { ImageSourceProvider.forUri(imageUriProvider.create(currentImageFormat)?.toString()) }
            },
            "Single ImageRequest" to {
                set { ImageSourceProvider.forImageRequest(ImageRequest.fromUri(imageUriProvider.create(currentImageFormat))) }
            },
            "Increasing quality" to {
                set {
                    ImageSourceProvider.increasingQuality(imageUriProvider.create(currentImageFormat), // TODO: low res
                            imageUriProvider.create(currentImageFormat))
                }
            },
            "First available" to {
                set {
                    ImageSourceProvider.firstAvailable(
                            ImageSourceProvider.forUri(imageUriProvider.create(currentImageFormat)),
                            ImageSourceProvider.forUri(imageUriProvider.create(currentImageFormat)))
                }
            },
            "Empty Image Source" to { set { ImageSourceProvider.emptySource() } },
            "null" to { set { null } }
    ), "Image Source")

    private fun set(create: () -> ImageSource?) {
        activeImageSourceProvider = {
            this.imageSource = create()
        }
        activeImageSourceProvider()
    }
}

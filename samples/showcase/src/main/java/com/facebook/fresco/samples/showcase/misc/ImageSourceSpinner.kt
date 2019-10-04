/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.misc

import android.net.Uri
import android.widget.Spinner
import com.facebook.fresco.samples.showcase.common.SpinnerUtils.setupWithCallbacks

object ImageSourceSpinner {

    @JvmOverloads
    fun Spinner.setup(
            imageUriProvider: ImageUriProvider,
            callback: (List<@JvmSuppressWildcards Uri>) -> Unit,
            numEntries: Int = 256) {
        setupWithCallbacks(
                listOf(
                        "Small images" to {
                            callback.invoke(
                                    imageUriProvider.getRandomSampleUris(
                                            ImageUriProvider.ImageSize.S, numEntries))
                        },
                        "Large images" to {
                            callback.invoke(
                                    imageUriProvider.getRandomSampleUris(
                                            ImageUriProvider.ImageSize.M, numEntries))
                        },
                        "Media" to {
                            callback.invoke(imageUriProvider.getMediaStoreUris(context))
                        },
                        "Empty list" to { callback.invoke(emptyList()) }
                )

        )
    }
}
/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.ktx

import android.net.Uri
import com.facebook.fresco.vito.source.ImageSource
import com.facebook.fresco.vito.source.ImageSourceProvider

object ImageSourceExtensions {

    fun Uri.asImageSource(): ImageSource = ImageSourceProvider.forUri(this)

    fun String.asImageSource(): ImageSource = ImageSourceProvider.forUri(this)

    fun ImageSource.withLowResImage(lowResImageSource: ImageSource): ImageSource = ImageSourceProvider.increasingQuality(lowResImageSource, this)
}

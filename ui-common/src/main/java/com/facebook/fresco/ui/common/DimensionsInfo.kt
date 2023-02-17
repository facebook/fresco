/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.ui.common

data class DimensionsInfo(
    val viewportWidth: Int,
    val viewportHeight: Int,
    val encodedImageWidth: Int,
    val encodedImageHeight: Int,
    val decodedImageWidth: Int,
    val decodedImageHeight: Int,
    val scaleType: String
)

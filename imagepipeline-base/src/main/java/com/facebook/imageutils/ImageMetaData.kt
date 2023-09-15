/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imageutils

import android.graphics.ColorSpace

/** Wrapper class representing the recovered meta data of an image when decoding. */
class ImageMetaData(width: Int, height: Int, val colorSpace: ColorSpace?) {

  val dimensions: Pair<Int, Int>? = if (width == -1 || height == -1) null else Pair(width, height)
}

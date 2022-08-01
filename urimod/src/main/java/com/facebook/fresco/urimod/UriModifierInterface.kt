/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.urimod

import android.net.Uri
import com.facebook.drawee.drawable.ScalingUtils.ScaleType

data class Dimensions(val w: Int, val h: Int) {
  override fun toString() = "${w}x${h}"
}

interface UriModifierInterface {
  fun modifyUri(uri: Uri, viewport: Dimensions?, scaleType: ScaleType): Uri
}

object NopUriModifier : UriModifierInterface {
  override fun modifyUri(uri: Uri, viewport: Dimensions?, scaleType: ScaleType): Uri = uri
}

object UriModifier {
  @JvmField var INSTANCE: UriModifierInterface = NopUriModifier
}

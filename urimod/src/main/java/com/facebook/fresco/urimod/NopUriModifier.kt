/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.urimod

import android.net.Uri
import com.facebook.drawee.drawable.ScalingUtils
import com.facebook.fresco.urimod.UriModifierInterface.ModificationResult

object NopUriModifier : UriModifierInterface {
  override fun modifyUri(
      uri: Uri,
      viewport: Dimensions?,
      scaleType: ScalingUtils.ScaleType?
  ): ModificationResult = ModificationResult.Disabled
}

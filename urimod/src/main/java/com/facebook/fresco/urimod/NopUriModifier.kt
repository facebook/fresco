/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.urimod

import android.net.Uri
import com.facebook.common.callercontext.ContextChain
import com.facebook.drawee.drawable.ScalingUtils
import com.facebook.fresco.urimod.UriModifierInterface.ModificationResult
import com.facebook.fresco.vito.source.UriImageSource

object NopUriModifier : UriModifierInterface {
  override fun modifyUri(
      imageSource: UriImageSource,
      viewport: Dimensions?,
      scaleType: ScalingUtils.ScaleType?,
      callerContext: Any?,
      contextChain: ContextChain?,
      forLoggingOnly: Boolean,
  ): ModificationResult = ModificationResult.Disabled("NopUriModifier")

  override fun modifyPrefetchUri(uri: Uri, callerContext: Any?): Uri = uri

  override fun unregisterReverseFallbackUri(uri: Uri) = Unit
}

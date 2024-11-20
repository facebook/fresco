/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.urimod

import android.net.Uri
import com.facebook.common.callercontext.ContextChain
import com.facebook.drawee.drawable.ScalingUtils.ScaleType
import com.facebook.fresco.vito.source.UriImageSource

interface UriModifierInterface {

  fun modifyUri(
      imageSource: UriImageSource,
      viewport: Dimensions?,
      scaleType: ScaleType?,
      callerContext: Any?,
      contextChain: ContextChain? = null,
      forLoggingOnly: Boolean = false,
  ): ModificationResult

  fun modifyPrefetchUri(uri: Uri, callerContext: Any?): Uri?

  fun unregisterReverseFallbackUri(uri: Uri)

  sealed class ModificationResult(private val comment: String) {

    abstract val bestAllowlistedSize: Int?

    override fun toString(): String = comment

    class Disabled(comment: String) : ModificationResult("Disabled:$comment") {
      override val bestAllowlistedSize: Int? = null
    }

    sealed class Modified(val newUri: Uri, comment: String) : ModificationResult(comment) {
      class ModifiedToAllowlistedSize(newUrl: Uri, override val bestAllowlistedSize: Int?) :
          Modified(newUrl, "ModifiedToAllowlistedSize")

      class ModifiedToMaxDimens(newUrl: Uri, override val bestAllowlistedSize: Int?) :
          Modified(newUrl, "ModifiedToMaxDimens")
    }

    data class FallbackToOriginalUrl(override val bestAllowlistedSize: Int?) :
        ModificationResult("FallbackToOriginalUrl")

    data class Unmodified(val reason: String, override val bestAllowlistedSize: Int?) :
        ModificationResult("Unmodified(reason='$reason'")

    data class FallbackToMbpMemoryCache(val isBestSize: String) :
        ModificationResult("FallbackToMbpMemoryCache($isBestSize") {
      override val bestAllowlistedSize: Int? = null
    }

    data class FallbackToMbpDiskCache(val isBestSize: Boolean) :
        ModificationResult("FallbackToMbpDiskCache(isBestSize=$isBestSize") {
      override val bestAllowlistedSize: Int? = null
    }
  }
}

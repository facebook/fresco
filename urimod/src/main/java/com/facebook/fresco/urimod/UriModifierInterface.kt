/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.urimod

import android.net.Uri
import com.facebook.drawee.drawable.ScalingUtils.ScaleType

interface UriModifierInterface {

  fun modifyUri(uri: Uri, viewport: Dimensions?, scaleType: ScaleType?): ModificationResult

  fun modifyPrefetchUri(uri: Uri, callerContext: Any?): Uri?

  sealed class ModificationResult(private val comment: String) {

    abstract val bestAllowlistedSize: Int?

    override fun toString(): String = comment

    object Disabled : ModificationResult("Disabled") {
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
  }
}

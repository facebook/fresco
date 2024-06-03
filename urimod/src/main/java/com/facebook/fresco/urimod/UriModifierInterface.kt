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

  /** This is used for IMAGE_LOAD_PERF logging. */
  sealed class ModificationResult(private val comment: String) {
    override fun toString(): String = comment

    object Disabled : ModificationResult("Disabled")

    sealed class Modified(val newUri: Uri, comment: String) : ModificationResult(comment) {
      class ModifiedToAllowlistedSize(newUrl: Uri) : Modified(newUrl, "ModifiedToAllowlistedSize")

      class ModifiedToMaxDimens(newUrl: Uri) : Modified(newUrl, "ModifiedToMaxDimens")
    }

    object FallbackToOriginalUrl : ModificationResult("FallbackToOriginalUrl")

    data class Unmodified(val reason: String) : ModificationResult("Unmodified(reason='$reason'")
  }
}

/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.urimod

import android.net.Uri
import com.facebook.drawee.drawable.ScalingUtils.ScaleType

const val NOT_MODIFIED_DISABLED: String = "Smart fetch is disabled"

interface UriModifierInterface {

  fun modifyUri(uri: Uri, viewport: Dimensions?, scaleType: ScaleType?): ModificationResult

  sealed class ModificationResult {
    object Disabled : ModificationResult() {
      override fun toString(): String {
        return "Disabled"
      }
    }

    data class Modified(val newUri: Uri) : ModificationResult() {
      override fun toString(): String {
        return "Modified"
      }
    }

    data class Unmodified(val reason: String) : ModificationResult() {
      override fun toString(): String {
        return "Unmodified(reason='$reason')"
      }
    }
  }
}

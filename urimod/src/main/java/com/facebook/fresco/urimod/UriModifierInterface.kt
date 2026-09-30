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
import com.facebook.drawee.drawable.Viewport
import com.facebook.fresco.vito.source.UriImageSource

interface UriModifierInterface {

  fun modifyUri(
      imageSource: UriImageSource,
      viewport: Dimensions?,
      scaleType: ScaleType?,
      callerContext: Any?,
      contextChain: ContextChain? = null,
  ): ModificationResult

  fun calculateBestUri(uri: Uri?, viewport: Viewport): Uri? = null

  fun modifyPrefetchUri(uri: Uri, callerContext: Any?): Uri?

  /**
   * Modifies the network uri adaptively based on current network or other conditions. No need to
   * provide viewport or scale type since those are extracted from the uri itself.
   *
   * @param isPrefetch true when nothing is waiting to display this image yet. Note that a prefetch
   *   can be joined by a real display request while it is still in flight, so this is only accurate
   *   as of the moment the fetch starts.
   * @param prefetchDestination which cache a prefetch is populating — `"bitmap"`, `"encoded"` or
   *   `"disk"`, the `HasExtraData.PREFETCH_DESTINATION_*` values — or null when this is not a
   *   prefetch or the destination was not recorded.
   */
  fun modifyNetworkUriForNetworkFetcher(
      uri: Uri,
      viewport: Viewport?,
      callerContext: Any?,
      contextChain: ContextChain? = null,
      isPrefetch: Boolean = false,
      prefetchDestination: String? = null,
  ): ModificationResult = ModificationResult.Disabled("Default")

  fun unregisterReverseFallbackUri(uri: Uri)

  sealed class ModificationResult(private val comment: String) {

    abstract val bestAllowlistedSize: Int?

    override fun toString(): String = comment

    class Disabled(comment: String) : ModificationResult("Disabled:$comment") {
      override val bestAllowlistedSize: Int? = null
    }

    sealed class Modified(val newUri: Uri, comment: String) : ModificationResult(comment) {

      var isVariation: Boolean = false

      class ModifiedToAllowlistedSize(newUrl: Uri, override val bestAllowlistedSize: Int?) :
          Modified(newUrl, "ModifiedToAllowlistedSize")

      class ModifiedToMaxDimens(newUrl: Uri, override val bestAllowlistedSize: Int?) :
          Modified(newUrl, "ModifiedToMaxDimens")

      class ModifiedScanEnd(newUrl: Uri) : Modified(newUrl, "ModifiedScanEnd") {
        override val bestAllowlistedSize: Int? = null
      }

      class ModifiedQuality(newUrl: Uri) : Modified(newUrl, "ModifiedQuality") {
        override val bestAllowlistedSize: Int? = null
      }

      class ModifiedByAiqConfig(newUrl: Uri) : Modified(newUrl, "ModifiedByAiqConfig") {
        override val bestAllowlistedSize: Int? = null
      }
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

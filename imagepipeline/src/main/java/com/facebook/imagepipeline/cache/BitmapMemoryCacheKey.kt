/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache

import android.net.Uri
import com.facebook.cache.common.CacheKey
import com.facebook.common.time.RealtimeSinceBootClock
import com.facebook.common.util.HashCodeUtil
import com.facebook.imagepipeline.common.ImageDecodeOptions
import com.facebook.imagepipeline.common.ResizeOptions
import com.facebook.imagepipeline.common.RotationOptions

/** Cache key for BitmapMemoryCache */
data class BitmapMemoryCacheKey(
    val sourceString: String,
    val resizeOptions: ResizeOptions?,
    val rotationOptions: RotationOptions,
    val imageDecodeOptions: ImageDecodeOptions,
    val postprocessorCacheKey: CacheKey?,
    val postprocessorName: String?,
) : CacheKey {

  var callerContext: Any? = null

  private val hash: Int =
      HashCodeUtil.hashCode(
          sourceString.hashCode(),
          resizeOptions?.hashCode() ?: 0,
          rotationOptions.hashCode(),
          imageDecodeOptions,
          postprocessorCacheKey,
          postprocessorName)
  val inBitmapCacheSince: Long = RealtimeSinceBootClock.get().now()

  override fun hashCode(): Int = hash

  override fun containsUri(uri: Uri): Boolean {
    return uriString.contains(uri.toString())
  }

  override fun getUriString(): String = sourceString

  override fun isResourceIdForDebugging() = false
}

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
import com.facebook.imagepipeline.common.ImageDecodeOptions
import com.facebook.imagepipeline.common.ResizeOptions
import com.facebook.imagepipeline.common.RotationOptions

/** Cache key for BitmapMemoryCache */
@Suppress("KtDataClass")
data class BitmapMemoryCacheKey(
    val sourceString: String,
    val resizeOptions: ResizeOptions?,
    val rotationOptions: RotationOptions,
    val imageDecodeOptions: ImageDecodeOptions,
    val postprocessorCacheKey: CacheKey?,
    val postprocessorName: String?,
) : CacheKey {

  private val hash: Int = run {
    var result = sourceString.hashCode()
    result = 31 * result + (resizeOptions?.hashCode() ?: 0)
    result = 31 * result + rotationOptions.hashCode()
    result = 31 * result + imageDecodeOptions.hashCode()
    result = 31 * result + (postprocessorCacheKey?.hashCode() ?: 0)
    result = 31 * result + (postprocessorName?.hashCode() ?: 0)
    result
  }
  val inBitmapCacheSince: Long = RealtimeSinceBootClock.get().now()

  override fun hashCode(): Int = hash

  override fun containsUri(uri: Uri): Boolean {
    return uriString.contains(uri.toString())
  }

  override fun getUriString(): String = sourceString

  override fun isResourceIdForDebugging(): Boolean = false

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (javaClass != other?.javaClass) {
      return false
    }

    val otherKey: BitmapMemoryCacheKey = other as BitmapMemoryCacheKey

    return sourceString == otherKey.sourceString &&
        resizeOptions == otherKey.resizeOptions &&
        rotationOptions == otherKey.rotationOptions &&
        imageDecodeOptions == otherKey.imageDecodeOptions &&
        postprocessorCacheKey == otherKey.postprocessorCacheKey &&
        postprocessorName == otherKey.postprocessorName
  }
}

/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.bitmap.cache

import android.net.Uri
import com.facebook.cache.common.CacheKey

/* Frame cache key for animation */

class AnimationFrameCacheKey
@JvmOverloads
constructor(imageId: Int, private val deepEquals: Boolean = false) : CacheKey {

  private val animationUriString: String = URI_PREFIX + imageId

  override fun containsUri(uri: Uri): Boolean = uri.toString().startsWith(animationUriString)

  override fun getUriString(): String = animationUriString

  override fun isResourceIdForDebugging(): Boolean = false

  override fun equals(o: Any?): Boolean {
    if (!deepEquals) {
      return super.equals(o)
    }

    if (this === o) {
      return true
    }
    if (o == null || javaClass != o.javaClass) {
      return false
    }

    val that = o as AnimationFrameCacheKey
    return animationUriString == that.animationUriString
  }

  override fun hashCode(): Int {
    if (!deepEquals) {
      return super.hashCode()
    }
    return animationUriString.hashCode()
  }

  companion object {
    private const val URI_PREFIX = "anim://"
  }
}

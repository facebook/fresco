/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core

import android.content.res.Resources
import com.facebook.cache.common.CacheKey
import com.facebook.common.internal.Objects
import com.facebook.fresco.middleware.Extras
import com.facebook.fresco.middleware.HasExtraData
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.fresco.vito.source.ImageSource
import com.facebook.imagepipeline.request.ImageRequest
import kotlin.jvm.JvmField

class VitoImageRequest(
    @JvmField val resources: Resources,
    @JvmField val imageSource: ImageSource,
    @JvmField val imageOptions: ImageOptions,
    @JvmField val logWithHighSamplingRate: Boolean = false,
    @JvmField val finalImageRequest: ImageRequest?,
    @JvmField val finalImageCacheKey: CacheKey?,
    @JvmField val extras: MutableMap<String, Any?> = mutableMapOf(),
) : HasExtraData {

  override fun <E> putExtra(key: String, value: E?) {
    extras[key] = value
  }

  @Suppress("UNCHECKED_CAST") override fun <E> getExtra(key: String): E? = extras[key] as? E

  override fun <E> getExtra(key: String, valueIfNotFound: E?): E? = getExtra(key) ?: valueIfNotFound

  override fun getExtras(): Extras = extras

  override fun putExtras(extras: Extras) = extras.forEach { putExtra(it.key, it.value) }

  override fun equals(obj: Any?): Boolean {
    if (this === obj) {
      return true
    }
    if (obj == null || javaClass != obj.javaClass) return false
    val other = obj as VitoImageRequest
    return resources === other.resources &&
        Objects.equal(imageSource, other.imageSource) &&
        Objects.equal(imageOptions, other.imageOptions)
  }

  fun equalsIfHasImage(other: VitoImageRequest?, hasImage: Boolean): Boolean {
    if (this === other) {
      return true
    }
    if (other == null) {
      return false
    }
    return resources === other.resources &&
        Objects.equal(imageSource, other.imageSource) &&
        if (hasImage) imageOptions.equalsForActualImage(other.imageOptions)
        else Objects.equal(imageOptions, other.imageOptions)
  }

  override fun hashCode(): Int {
    var result = resources.hashCode()
    result = 31 * result + imageSource.hashCode()
    result = 31 * result + imageOptions.hashCode()
    return result
  }
}

/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.source

import android.net.Uri

@Suppress("KtDataClass")
data class SingleImageSourceImpl(
    override val uri: Uri,
    override val extras: Map<String, Any>? = null
) : SingleImageSource {

  fun getExtra(key: String): Any? = extras?.get(key)

  override fun getStringExtra(key: String): String? = getExtra(key) as? String

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (javaClass != other?.javaClass) {
      return false
    }

    val otherImageSource: SingleImageSourceImpl = other as SingleImageSourceImpl

    return imageUri == otherImageSource.imageUri && extras == otherImageSource.extras
  }

  override fun hashCode(): Int {
    var result = imageUri.hashCode()
    result = 31 * result + (extras?.hashCode() ?: 0)
    return result
  }

  override val imageUri: Uri = uri
}

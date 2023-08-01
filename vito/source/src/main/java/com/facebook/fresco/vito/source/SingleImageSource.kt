/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.source

import android.net.Uri

@Suppress("KtDataClass")
data class SingleImageSource(val uri: Uri, val extras: Map<String, Any>? = null) : ImageSource {

  fun getExtra(key: String): Any? = extras?.get(key)

  fun getStringExtra(key: String): String? = getExtra(key) as? String

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (javaClass != other?.javaClass) {
      return false
    }

    val otherImageSource: SingleImageSource = other as SingleImageSource

    if (uri != otherImageSource.uri) {
      return false
    }
    if (extras != otherImageSource.extras) {
      return false
    }

    return true
  }

  override fun hashCode(): Int {
    var result = uri.hashCode()
    result = 31 * result + (extras?.hashCode() ?: 0)
    return result
  }
}

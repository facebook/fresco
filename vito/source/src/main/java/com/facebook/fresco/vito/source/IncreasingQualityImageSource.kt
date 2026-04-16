/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.source

@Suppress("KtDataClass")
data class IncreasingQualityImageSource(
    val lowResSource: ImageSource,
    val highResSource: ImageSource,
    val extras: Map<String, Any>? = null,
    val markSuccessOnLowRes: Boolean = false,
) : ImageSource {

  fun getExtra(key: String): Any? = extras?.get(key)

  fun getStringExtra(key: String): String? = getExtra(key) as? String

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (javaClass != other?.javaClass) {
      return false
    }

    val otherImageSource: IncreasingQualityImageSource = other as IncreasingQualityImageSource

    return lowResSource == otherImageSource.lowResSource &&
        highResSource == otherImageSource.highResSource &&
        extras == otherImageSource.extras &&
        markSuccessOnLowRes == otherImageSource.markSuccessOnLowRes
  }

  override fun hashCode(): Int {
    var result = lowResSource.hashCode()
    result = 31 * result + highResSource.hashCode()
    result = 31 * result + (extras?.hashCode() ?: 0)
    result = 31 * result + markSuccessOnLowRes.hashCode()
    return result
  }

  override fun getClassNameString(): String = "IncreasingQualityImageSource"
}

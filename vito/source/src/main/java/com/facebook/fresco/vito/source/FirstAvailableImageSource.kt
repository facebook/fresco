/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.source

@Suppress("KtDataClass")
data class FirstAvailableImageSource(val imageSources: Array<out ImageSource>) : ImageSource {

  override fun equals(other: Any?): Boolean {
    if (ImageSourceConfig.doNotUseOverriddenDataClassMembers) {
      return super.equals(other)
    }

    if (this === other) {
      return true
    }
    if (javaClass != other?.javaClass) {
      return false
    }

    return imageSources.contentEquals((other as FirstAvailableImageSource).imageSources)
  }

  override fun hashCode(): Int {
    return if (ImageSourceConfig.doNotUseOverriddenDataClassMembers) {
      super.hashCode()
    } else {
      imageSources.contentHashCode()
    }
  }
}

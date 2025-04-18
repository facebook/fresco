/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.source

import android.net.Uri
import com.facebook.drawee.drawable.SizingHint

class SmartImageSource(
    override val imageUri: Uri,
    val sizingHint: SizingHint = SizingHint.DEFAULT,
    override val extras: Map<String, Any>? = null,
) : SingleImageSource {
  override val uri: Uri = imageUri

  override fun getStringExtra(key: String): String? {
    return extras?.get(key) as? String
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (javaClass != other?.javaClass) {
      return false
    }

    if (other !is SmartImageSource) {
      return false
    }

    if (imageUri != other.imageUri) {
      return false
    }
    if (sizingHint != other.sizingHint) {
      return false
    }
    return extras == other.extras
  }

  override fun hashCode(): Int {
    var result = imageUri.hashCode()
    result = 31 * result + sizingHint.hashCode()
    result = 31 * result + (extras?.hashCode() ?: 0)
    return result
  }
}

/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.media

import android.webkit.MimeTypeMap
import com.facebook.common.internal.ImmutableMap

/** Wrapper around the system's [MimeTypeMap] that also handles types it doesn't support. */
object MimeTypeMapWrapper {

  private val mimeTypeMap: MimeTypeMap = MimeTypeMap.getSingleton()

  private val mimeTypeToExtensionMap: Map<String, String> =
      ImmutableMap.of("image/heif", "heif", "image/heic", "heic")

  private val extensionToMimeTypeMap: Map<String, String> =
      ImmutableMap.of("heif", "image/heif", "heic", "image/heic")

  @JvmStatic
  fun getExtensionFromMimeType(mimeType: String): String? {
    val result = mimeTypeToExtensionMap[mimeType]
    if (result != null) {
      return result
    }
    return mimeTypeMap.getExtensionFromMimeType(mimeType)
  }

  @JvmStatic
  fun getMimeTypeFromExtension(extension: String): String? {
    val result = extensionToMimeTypeMap[extension]
    if (result != null) {
      return result
    }
    return mimeTypeMap.getMimeTypeFromExtension(extension)
  }

  @JvmStatic
  fun hasExtension(extension: String): Boolean =
      extensionToMimeTypeMap.containsKey(extension) || mimeTypeMap.hasExtension(extension)

  @JvmStatic
  fun hasMimeType(mimeType: String): Boolean =
      mimeTypeToExtensionMap.containsKey(mimeType) || mimeTypeMap.hasMimeType(mimeType)
}

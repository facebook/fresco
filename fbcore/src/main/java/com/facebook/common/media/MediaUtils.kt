/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.media

import androidx.annotation.NonNull
import java.util.Locale

/** Utility class. */
object MediaUtils {

  @JvmField // Additional mime types that we know to be a particular media type but which may not be
  // supported natively on the device.
  val ADDITIONAL_ALLOWED_MIME_TYPES =
      mapOf(
          "mkv" to "video/x-matroska",
          "glb" to "model/gltf-binary",
      )

  @JvmStatic fun isPhoto(mimeType: String?): Boolean = mimeType?.startsWith("image/") ?: false

  @JvmStatic fun isVideo(mimeType: String?): Boolean = mimeType?.startsWith("video/") ?: false

  @JvmStatic fun isThreeD(mimeType: String?): Boolean = mimeType == "model/gltf-binary"

  @JvmStatic
  fun extractMime(@NonNull path: String): String? {
    val extension = extractExtension(path)?.toLowerCase(Locale.US) ?: return null

    // If we did not find a mime type for the extension specified, check our additional
    // extension/mime-type mappings.
    return MimeTypeMapWrapper.getMimeTypeFromExtension(extension)
        ?: ADDITIONAL_ALLOWED_MIME_TYPES[extension]
  }

  private fun extractExtension(@NonNull path: String): String? {
    val pos = path.lastIndexOf('.')
    return if (pos < 0 || pos == path.length - 1) null else path.substring(pos + 1)
  }

  /**
   * @return true if the mime type is one of our whitelisted mimetypes that we support beyond what
   *   the native platform supports.
   */
  @JvmStatic
  fun isNonNativeSupportedMimeType(@NonNull mimeType: String): Boolean {
    return ADDITIONAL_ALLOWED_MIME_TYPES.containsValue(mimeType)
  }
}

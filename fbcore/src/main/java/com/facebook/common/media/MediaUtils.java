/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.media;

import com.facebook.common.internal.ImmutableMap;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Utility class.
 */
public class MediaUtils {
  // Additional mime types that we know to be a particular media type but which may not be
  // supported natively on the device.
  public static final Map<String, String> ADDITIONAL_ALLOWED_MIME_TYPES =
      ImmutableMap.of("mkv", "video/x-matroska", "glb", "model/gltf-binary");

  public static boolean isPhoto(@Nullable String mimeType) {
    return mimeType != null && mimeType.startsWith("image/");
  }

  public static boolean isVideo(@Nullable String mimeType) {
    return mimeType != null && mimeType.startsWith("video/");
  }

  public static boolean isThreeD(@Nullable String mimeType) {
    return mimeType != null && mimeType.equals("model/gltf-binary");
  }

  public @Nullable static String extractMime(String path) {
    String extension = extractExtension(path);
    if (extension == null) {
      return null;
    }

    extension = extension.toLowerCase(Locale.US);
    String mimeType = MimeTypeMapWrapper.getMimeTypeFromExtension(extension);

    // If we did not find a mime type for the extension specified, check our additional
    // extension/mime-type mappings.
    if (mimeType == null) {
      mimeType = ADDITIONAL_ALLOWED_MIME_TYPES.get(extension);
    }
    return mimeType;
  }

  private @Nullable static String extractExtension(String path) {
    int pos = path.lastIndexOf('.');
    if (pos < 0 || pos == path.length() - 1) {
      return null;
    }
    return path.substring(pos + 1);
  }

  /**
   * @return true if the mime type is one of our whitelisted mimetypes that we support beyond
   *         what the native platform supports.
   */
  public static boolean isNonNativeSupportedMimeType(String mimeType) {
    return ADDITIONAL_ALLOWED_MIME_TYPES.containsValue(mimeType);
  }
}

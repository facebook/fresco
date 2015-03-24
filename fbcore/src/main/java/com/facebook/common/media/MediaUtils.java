/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.common.media;

import javax.annotation.Nullable;

import java.util.Locale;
import java.util.Map;

import android.webkit.MimeTypeMap;

import com.facebook.common.internal.ImmutableMap;

/**
 * Utility class.
 */
public class MediaUtils {
  // Additional mime types that we know to be a particular media type but which may not be
  // supported natively on the device.
  public static final Map<String, String> ADDITIONAL_ALLOWED_MIME_TYPES =
      ImmutableMap.of("mkv", "video/x-matroska");

  public static boolean isPhoto(@Nullable String mimeType) {
    return mimeType != null && mimeType.startsWith("image/");
  }

  public static boolean isVideo(@Nullable String mimeType) {
    return mimeType != null && mimeType.startsWith("video/");
  }

  public @Nullable static String extractMime(String path) {
    String extension = extractExtension(path);
    if (extension == null) {
      return null;
    }
    extension = extension.toLowerCase(Locale.US);
    String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);

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

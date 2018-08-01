/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.common.media;

import android.webkit.MimeTypeMap;
import com.facebook.common.internal.ImmutableMap;
import java.util.Map;

/** Wrapper around the system's {@link MimeTypeMap} that also handles types it doesn't support. */
public class MimeTypeMapWrapper {

  private static final MimeTypeMap sMimeTypeMap = MimeTypeMap.getSingleton();

  private static final Map<String, String> sMimeTypeToExtensionMap =
      ImmutableMap.of(
          "image/heif", "heif",
          "image/heic", "heic");

  private static final Map<String, String> sExtensionToMimeTypeMap =
      ImmutableMap.of(
          "heif", "image/heif",
          "heic", "image/heic");

  public static String getExtensionFromMimeType(String mimeType) {
    String result = sMimeTypeToExtensionMap.get(mimeType);
    if (result != null) {
      return result;
    }
    return sMimeTypeMap.getExtensionFromMimeType(mimeType);
  }

  public static String getMimeTypeFromExtension(String extension) {
    String result = sExtensionToMimeTypeMap.get(extension);
    if (result != null) {
      return result;
    }
    return sMimeTypeMap.getMimeTypeFromExtension(extension);
  }

  public static boolean hasExtension(String extension) {
    return sExtensionToMimeTypeMap.containsKey(extension) || sMimeTypeMap.hasExtension(extension);
  }

  public static boolean hasMimeType(String mimeType) {
    return sMimeTypeToExtensionMap.containsKey(mimeType) || sMimeTypeMap.hasMimeType(mimeType);
  }
}

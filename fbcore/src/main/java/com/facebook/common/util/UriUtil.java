/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.common.util;

import javax.annotation.Nullable;

import android.net.Uri;

public class UriUtil {

  /**
   * http scheme for URIs
   */
  public static final String HTTP_SCHEME = "http";
  public static final String HTTPS_SCHEME = "https";

  /**
   * File scheme for URIs
   */
  public static final String LOCAL_FILE_SCHEME = "file";

  /**
   * Content URI scheme for URIs
   */
  public static final String LOCAL_CONTENT_SCHEME = "content";

  /**
   * Asset scheme for URIs
   */
  public static final String LOCAL_ASSET_SCHEME = "asset";

  /**
   * Resource scheme for URIs
   */
  public static final String LOCAL_RESOURCE_SCHEME = "res";

  /** Data scheme for URIs */
  public static final String DATA_SCHEME = "data";

  /**
   * /**
   * Check if uri represents network resource
   *
   * @param uri uri to check
   * @return true if uri's scheme is equal to "http" or "https"
   */
  public static boolean isNetworkUri(@Nullable Uri uri) {
    final String scheme = getSchemeOrNull(uri);
    return HTTPS_SCHEME.equals(scheme) || HTTP_SCHEME.equals(scheme);
  }

  /**
   * Check if uri represents local file
   *
   * @param uri uri to check
   * @return true if uri's scheme is equal to "file"
   */
  public static boolean isLocalFileUri(@Nullable Uri uri) {
    final String scheme = getSchemeOrNull(uri);
    return LOCAL_FILE_SCHEME.equals(scheme);
  }

  /**
   * Check if uri represents local content
   *
   * @param uri uri to check
   * @return true if uri's scheme is equal to "content"
   */
  public static boolean isLocalContentUri(@Nullable Uri uri) {
    final String scheme = getSchemeOrNull(uri);
    return LOCAL_CONTENT_SCHEME.equals(scheme);
  }

  /**
   * Check if uri represents local asset
   *
   * @param uri uri to check
   * @return true if uri's scheme is equal to "asset"
   */
  public static boolean isLocalAssetUri(@Nullable Uri uri) {
    final String scheme = getSchemeOrNull(uri);
    return LOCAL_ASSET_SCHEME.equals(scheme);
  }

  /**
   * Check if uri represents local resource
   *
   * @param uri uri to check
   * @return true if uri's scheme is equal to {@link #LOCAL_RESOURCE_SCHEME}
   */
  public static boolean isLocalResourceUri(@Nullable Uri uri) {
    final String scheme = getSchemeOrNull(uri);
    return LOCAL_RESOURCE_SCHEME.equals(scheme);
  }

  /** Check if the uri is a data uri */
  public static boolean isDataUri(@Nullable Uri uri) {
    return DATA_SCHEME.equals(getSchemeOrNull(uri));
  }

  /**
   * @param uri uri to extract scheme from, possibly null
   * @return null if uri is null, result of uri.getScheme() otherwise
   */
  @Nullable
  public static String getSchemeOrNull(@Nullable Uri uri) {
    return uri == null ? null : uri.getScheme();
  }

  /**
   * A wrapper around {@link Uri#parse} that returns null if the input is null.
   *
   * @param uriAsString the uri as a string
   * @return the parsed Uri or null if the input was null
   */
  public static Uri parseUriOrNull(@Nullable String uriAsString) {
    return uriAsString != null ? Uri.parse(uriAsString) : null;
  }
}

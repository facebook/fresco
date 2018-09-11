/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.cache.common;

import com.facebook.common.util.SecureHashUtil;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public final class CacheKeyUtil {

  /**
   * Get a list of possible resourceIds from MultiCacheKey or get single resourceId from CacheKey.
   */
  public static List<String> getResourceIds(final CacheKey key) {
    try {
      final List<String> ids;
      if (key instanceof MultiCacheKey) {
        List<CacheKey> keys = ((MultiCacheKey) key).getCacheKeys();
        ids = new ArrayList<>(keys.size());
        for (int i = 0; i < keys.size(); i++) {
          ids.add(secureHashKey(keys.get(i)));
        }
      } else {
        ids = new ArrayList<>(1);
        ids.add(secureHashKey(key));
      }
      return ids;
    } catch (UnsupportedEncodingException e) {
      // This should never happen. All VMs support UTF-8
      throw new RuntimeException(e);
    }
  }

  /**
   * Get the resourceId from the first key in MultiCacheKey or get single resourceId from CacheKey.
   */
  public static String getFirstResourceId(final CacheKey key) {
    try {
      if (key instanceof MultiCacheKey) {
        List<CacheKey> keys = ((MultiCacheKey) key).getCacheKeys();
        return secureHashKey(keys.get(0));
      } else {
        return secureHashKey(key);
      }
    } catch (UnsupportedEncodingException e) {
      // This should never happen. All VMs support UTF-8
      throw new RuntimeException(e);
    }
  }

  private static String secureHashKey(final CacheKey key) throws UnsupportedEncodingException {
    return SecureHashUtil.makeSHA1HashBase64(key.getUriString().getBytes("UTF-8"));
  }
}

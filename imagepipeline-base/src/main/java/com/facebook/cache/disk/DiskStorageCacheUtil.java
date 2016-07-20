/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.cache.disk;

import javax.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import android.content.SharedPreferences;

import com.facebook.common.logging.FLog;

public final class DiskStorageCacheUtil {

  private static final String TAG = DiskStorageCacheUtil.class.getSimpleName();

  private DiskStorageCacheUtil() {
  }

  protected static void addDiskCacheEntry(
      Integer hashKey,
      String resourceId,
      SharedPreferences sharedPreferences) {
    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putString(String.valueOf(hashKey), resourceId);
    editor.apply();
  }

  protected static void deleteDiskCacheEntry(
      Integer hashKey,
      SharedPreferences sharedPreferences) {
    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.remove(String.valueOf(hashKey));
    editor.apply();
  }

  protected synchronized static Map<Integer, String> readStoredIndex(
      @Nullable SharedPreferences sharedPreferences,
      Set<String> resourceIndex) {
    Map<Integer, String> index = new HashMap<>();
    if (sharedPreferences == null) {
      return index;
    }
    Map<String, ?> allEntries = sharedPreferences.getAll();
    SharedPreferences.Editor editor = sharedPreferences.edit();
    for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
      if (entry.getValue() instanceof String) {
        Integer key = Integer.parseInt(entry.getKey());
        if (resourceIndex.contains(entry.getValue())) {
          index.put(key, (String) entry.getValue());
        } else {
          editor.remove(String.valueOf(entry.getKey()));
        }
      } else {
        FLog.e(TAG, "SharedPreference doesn't store right data type");
      }
    }
    editor.apply();
    return index;
  }

  protected static void clearDiskEntries(SharedPreferences sharedPreferences) {
    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.clear();
    editor.apply();
  }
}

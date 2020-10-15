/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.cache.common;

import android.net.Uri;
import com.facebook.common.internal.Preconditions;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

/**
 * {@link CacheKey} implementation that is a simple wrapper around a {@link String} object.
 *
 * <p>Users of CacheKey should construct it by providing a unique string that unambiguously
 * identifies the cached resource.
 */
@Nullsafe(Nullsafe.Mode.STRICT)
public class SimpleCacheKey implements CacheKey {
  final String mKey;
  final boolean mIsResourceIdForDebugging;

  public SimpleCacheKey(final String key) {
    this(key, false);
  }

  public SimpleCacheKey(final String key, boolean isResourceIdForDebugging) {
    mKey = Preconditions.checkNotNull(key);
    mIsResourceIdForDebugging = isResourceIdForDebugging;
  }

  @Override
  public String toString() {
    return mKey;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof SimpleCacheKey) {
      final SimpleCacheKey otherKey = (SimpleCacheKey) o;
      return mKey.equals(otherKey.mKey);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return mKey.hashCode();
  }

  @Override
  public boolean containsUri(Uri uri) {
    return mKey.contains(uri.toString());
  }

  @Override
  public String getUriString() {
    return mKey;
  }

  @Override
  public boolean isResourceIdForDebugging() {
    return mIsResourceIdForDebugging;
  }
}

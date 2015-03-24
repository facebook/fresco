/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.cache;

import javax.annotation.concurrent.GuardedBy;

import java.util.Map;

import com.facebook.common.internal.Maps;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.logging.FLog;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.cache.common.CacheKey;

/**
 * This is class encapsulates Map that maps ImageCacheKeys to SharedReferences pointing to
 * PooledByteBuffers. It is used by SimpleImageCache to store values that are being written
 * to disk cache, so that they can be returned by parallel cache get operations.
 */
public class StagingArea {
  private static final Class<?> TAG = StagingArea.class;

  @GuardedBy("this")
  private Map<CacheKey, CloseableReference<PooledByteBuffer>> mMap;

  private StagingArea() {
    mMap = Maps.newHashMap();
  }

  public static StagingArea getInstance() {
    return new StagingArea();
  }

  /**
   * Stores key-value in this StagingArea. This call overrides previous value
   * of stored reference if
   * @param key
   * @param bufferRef reference to be associated with key
   */
  public synchronized void put(
      final CacheKey key,
      final CloseableReference<PooledByteBuffer> bufferRef) {
    Preconditions.checkNotNull(key);
    Preconditions.checkArgument(CloseableReference.isValid(bufferRef));

    // we're making a 'copy' of this reference - so duplicate it
    final CloseableReference<?> oldEntry = mMap.put(key, bufferRef.clone());
    if (oldEntry != null) {
      oldEntry.close();
    }
    logStats();
  }

  /**
   * Removes key-value from the StagingArea. Both key and value must match.
   * @param key
   * @param bufferRef value corresponding to key
   * @return true if item was removed
   */
  public synchronized boolean remove(
      final CacheKey key,
      final CloseableReference<PooledByteBuffer> bufferRef) {
    Preconditions.checkNotNull(key);
    Preconditions.checkNotNull(bufferRef);
    Preconditions.checkArgument(CloseableReference.isValid(bufferRef));

    final CloseableReference<?> oldValue = mMap.get(key);

    if (oldValue == null || oldValue.get() != bufferRef.get()) {
      return false;
    }

    mMap.remove(key);
    oldValue.close();
    logStats();
    return true;
  }

  /**
   * @param key
   * @return value associated with given key or null if no value is associated
   */
  public synchronized CloseableReference<PooledByteBuffer> get(final CacheKey key) {
    Preconditions.checkNotNull(key);
    CloseableReference<PooledByteBuffer> storedRef = mMap.get(key);
    if (storedRef != null) {
      synchronized (storedRef) {
        if (!CloseableReference.isValid(storedRef)) {
          // Reference is not valid, this means that someone cleared reference while it was still in
          // use. Log error
          // TODO: 3697790
          mMap.remove(key);
          FLog.w(
              TAG,
              "Found closed reference %d for key %s (%d)",
              System.identityHashCode(storedRef),
              key.toString(),
              System.identityHashCode(key));
          return null;
        }
        storedRef = storedRef.clone();
      }
    }
    return storedRef;
  }

  /**
   * Simple 'debug' logging of stats.
   */
  private synchronized void logStats() {
    FLog.v(TAG, "Count = %d", mMap.size());
  }

}

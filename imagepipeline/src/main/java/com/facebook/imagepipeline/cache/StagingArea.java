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

import java.util.HashMap;
import java.util.Map;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.logging.FLog;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.cache.common.CacheKey;

/**
 * This is class encapsulates Map that maps ImageCacheKeys to EncodedImages pointing to
 * PooledByteBuffers. It is used by SimpleImageCache to store values that are being written
 * to disk cache, so that they can be returned by parallel cache get operations.
 */
public class StagingArea {
  private static final Class<?> TAG = StagingArea.class;

  @GuardedBy("this")
  private Map<CacheKey, EncodedImage> mMap;

  private StagingArea() {
    mMap = new HashMap<>();
  }

  public static StagingArea getInstance() {
    return new StagingArea();
  }

  /**
   * Stores key-value in this StagingArea. This call overrides previous value
   * of stored reference if
   * @param key
   * @param encodedImage EncodedImage to be associated with key
   */
  public synchronized void put(final CacheKey key, final EncodedImage encodedImage) {
    Preconditions.checkNotNull(key);
    Preconditions.checkArgument(EncodedImage.isValid(encodedImage));

    // we're making a 'copy' of this reference - so duplicate it
    final EncodedImage oldEntry = mMap.put(key, EncodedImage.cloneOrNull(encodedImage));
    EncodedImage.closeSafely(oldEntry);
    logStats();
  }

  /**
   * Removes key-value from the StagingArea. Both key and value must match.
   * @param key
   * @param encodedImage value corresponding to key
   * @return true if item was removed
   */
  public synchronized boolean remove(final CacheKey key, final EncodedImage encodedImage) {
    Preconditions.checkNotNull(key);
    Preconditions.checkNotNull(encodedImage);
    Preconditions.checkArgument(EncodedImage.isValid(encodedImage));

    final EncodedImage oldValue = mMap.get(key);

    if (oldValue == null) {
      return false;
    }

    CloseableReference<PooledByteBuffer> oldRef = oldValue.getByteBufferRef();
    CloseableReference<PooledByteBuffer> ref = encodedImage.getByteBufferRef();
    try {
      if (oldRef == null || ref == null || oldRef.get() != ref.get()) {
        return false;
      }
      mMap.remove(key);
    } finally {
      CloseableReference.closeSafely(ref);
      CloseableReference.closeSafely(oldRef);
      EncodedImage.closeSafely(oldValue);
    }

    logStats();
    return true;
  }

  /**
   * @param key
   * @return value associated with given key or null if no value is associated
   */
  public synchronized EncodedImage get(final CacheKey key) {
    Preconditions.checkNotNull(key);
    EncodedImage storedEncodedImage = mMap.get(key);
    if (storedEncodedImage != null) {
      synchronized (storedEncodedImage) {
        if (!EncodedImage.isValid(storedEncodedImage)) {
          // Reference is not valid, this means that someone cleared reference while it was still in
          // use. Log error
          // TODO: 3697790
          mMap.remove(key);
          FLog.w(
              TAG,
              "Found closed reference %d for key %s (%d)",
              System.identityHashCode(storedEncodedImage),
              key.toString(),
              System.identityHashCode(key));
          return null;
        }
        storedEncodedImage = EncodedImage.cloneOrNull(storedEncodedImage);
      }
    }
    return storedEncodedImage;
  }

  /**
   * Simple 'debug' logging of stats.
   */
  private synchronized void logStats() {
    FLog.v(TAG, "Count = %d", mMap.size());
  }

}

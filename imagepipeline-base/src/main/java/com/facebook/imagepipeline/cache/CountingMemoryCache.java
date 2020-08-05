/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache;

import android.graphics.Bitmap;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.memory.MemoryTrimmable;
import com.facebook.common.references.CloseableReference;
import java.util.Map;
import javax.annotation.Nullable;

public interface CountingMemoryCache<K, V> extends MemoryCache<K, V>, MemoryTrimmable {

  @Nullable
  CloseableReference<V> cache(
      K key, CloseableReference<V> valueRef, EntryStateObserver<K> observer);

  @Nullable
  CloseableReference<V> reuse(K key);

  /**
   * Removes the exclusively owned items until the cache constraints are met.
   *
   * <p>This method invokes the external {@link CloseableReference#close} method, so it must not be
   * called while holding the <code>this</code> lock.
   */
  void maybeEvictEntries();

  /** Gets the total size in bytes of the cached items that are used by at least one client. */
  int getInUseSizeInBytes();

  /** Gets the total size in bytes of the cached items that are used by at least one client. */
  int getEvictionQueueCount();

  /** Gets the total size in bytes of the exclusively owned items. */
  int getEvictionQueueSizeInBytes();

  /** Removes all the items from the cache. */
  void clear();

  MemoryCacheParams getMemoryCacheParams();

  CountingLruMap<K, Entry<K, V>> getCachedEntries();

  Map<Bitmap, Object> getOtherEntries();

  /** Interface used to observe the state changes of an entry. */
  public interface EntryStateObserver<K> {

    /**
     * Called when the exclusivity status of the entry changes.
     *
     * <p>The item can be reused if it is exclusively owned by the cache.
     */
    void onExclusivityChanged(K key, boolean isExclusive);
  }

  /** The internal representation of a key-value pair stored by the cache. */
  @VisibleForTesting
  class Entry<K, V> {
    public final K key;
    public final CloseableReference<V> valueRef;
    // The number of clients that reference the value.
    public int clientCount;
    // Whether or not this entry is tracked by this cache. Orphans are not tracked by the cache and
    // as soon as the last client of an orphaned entry closes their reference, the entry's copy is
    // closed too.
    public boolean isOrphan;
    @Nullable public final EntryStateObserver<K> observer;

    private Entry(K key, CloseableReference<V> valueRef, @Nullable EntryStateObserver<K> observer) {
      this.key = Preconditions.checkNotNull(key);
      this.valueRef = Preconditions.checkNotNull(CloseableReference.cloneOrNull(valueRef));
      this.clientCount = 0;
      this.isOrphan = false;
      this.observer = observer;
    }

    /** Creates a new entry with the usage count of 0. */
    @VisibleForTesting
    public static <K, V> CountingMemoryCache.Entry<K, V> of(
        final K key,
        final CloseableReference<V> valueRef,
        final @Nullable EntryStateObserver<K> observer) {
      return new Entry<>(key, valueRef, observer);
    }
  }
}

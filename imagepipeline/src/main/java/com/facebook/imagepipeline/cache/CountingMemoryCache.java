/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.cache;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import android.os.SystemClock;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.Supplier;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.memory.MemoryTrimType;
import com.facebook.common.memory.MemoryTrimmable;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.references.ResourceReleaser;

import com.android.internal.util.Predicate;

/**
 * Layer of memory cache stack responsible for managing eviction of the the cached items.
 *
 * <p> This layer is responsible for LRU eviction strategy and for maintaining the size boundaries
 * of the cached items.
 *
 * <p> Only the exclusively owned elements, i.e. the elements not referenced by any client, can be
 * evicted.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
@ThreadSafe
public class CountingMemoryCache<K, V> implements MemoryCache<K, V>, MemoryTrimmable {

  /**
   * Interface used to specify the trimming strategy for the cache.
   */
  public static interface CacheTrimStrategy {
    double getTrimRatio(MemoryTrimType trimType);
  }

  /**
   * The internal representation of a key-value pair stored by the cache.
   */
  @VisibleForTesting
  static class Entry<K, V> {
    public final K key;
    public final CloseableReference<V> valueRef;
    // The number of clients that reference the value.
    public int clientCount;
    // Whether or not this entry is tracked by this cache. Orphans are not tracked by the cache and
    // as soon as the last client of an orphaned entry closes their reference, the entry's copy is
    // closed too.
    public boolean isOrphan;

    private Entry(K key, CloseableReference<V> valueRef) {
      this.key = Preconditions.checkNotNull(key);
      this.valueRef = Preconditions.checkNotNull(CloseableReference.cloneOrNull(valueRef));
      this.clientCount = 0;
      this.isOrphan = false;
    }

    /** Creates a new entry with the usage count of 0. */
    @VisibleForTesting
    static <K, V> Entry<K, V> of(final K key, final CloseableReference<V> valueRef) {
      return new Entry<>(key, valueRef);
    }
  }

  // How often the cache checks for a new cache configuration.
  @VisibleForTesting
  static final long PARAMS_INTERCHECK_INTERVAL_MS = TimeUnit.MINUTES.toMillis(5);

  // Contains the items that are not being used by any client and are hence viable for eviction.
  @GuardedBy("this")
  @VisibleForTesting
  final CountingLruMap<K, Entry<K, V>> mExclusiveEntries;

  // Contains all the cached items including the exclusively owned ones.
  @GuardedBy("this")
  @VisibleForTesting
  final CountingLruMap<K, Entry<K, V>> mCachedEntries;

  private final ValueDescriptor<V> mValueDescriptor;

  private final CacheTrimStrategy mCacheTrimStrategy;

  // Cache size constraints.
  private final Supplier<MemoryCacheParams> mMemoryCacheParamsSupplier;
  @GuardedBy("this")
  protected MemoryCacheParams mMemoryCacheParams;
  @GuardedBy("this")
  private long mLastCacheParamsCheck;

  public CountingMemoryCache(
      ValueDescriptor<V> valueDescriptor,
      CacheTrimStrategy cacheTrimStrategy,
      Supplier<MemoryCacheParams> memoryCacheParamsSupplier) {
    mValueDescriptor = valueDescriptor;
    mExclusiveEntries = new CountingLruMap<>(wrapValueDescriptor(valueDescriptor));
    mCachedEntries = new CountingLruMap<>(wrapValueDescriptor(valueDescriptor));
    mCacheTrimStrategy = cacheTrimStrategy;
    mMemoryCacheParamsSupplier = memoryCacheParamsSupplier;
    mMemoryCacheParams = mMemoryCacheParamsSupplier.get();
    mLastCacheParamsCheck = SystemClock.elapsedRealtime();
  }

  private ValueDescriptor<Entry<K, V>> wrapValueDescriptor(
      final ValueDescriptor<V> evictableValueDescriptor) {
    return new ValueDescriptor<Entry<K,V>>() {
      @Override
      public int getSizeInBytes(Entry<K, V> entry) {
        return evictableValueDescriptor.getSizeInBytes(entry.valueRef.get());
      }
    };
  }

  /**
   * Caches the given key-value pair.
   *
   * <p> Important: the client should use the returned reference instead of the original one.
   * It is the caller's responsibility to close the returned reference once not needed anymore.
   *
   * @return the new reference to be used, null if the value cannot be cached
   */
  public CloseableReference<V> cache(final K key, final CloseableReference<V> valueRef) {
    Preconditions.checkNotNull(key);
    Preconditions.checkNotNull(valueRef);

    maybeUpdateCacheParams();

    CloseableReference<V> oldRefToClose = null;
    CloseableReference<V> clientRef = null;
    synchronized (this) {
      // remove the old item (if any) as it is stale now
      mExclusiveEntries.remove(key);
      Entry<K, V> oldEntry = mCachedEntries.remove(key);
      if (oldEntry != null) {
        makeOrphan(oldEntry);
        oldRefToClose = referenceToClose(oldEntry);
      }

      if (canCacheNewValue(valueRef.get())) {
        Entry<K, V> newEntry = Entry.of(key, valueRef);
        mCachedEntries.put(key, newEntry);
        clientRef = newClientReference(newEntry);
      }
    }
    CloseableReference.closeSafely(oldRefToClose);

    maybeEvictEntries();
    return clientRef;
  }

  /** Checks the cache constraints to determine whether the new value can be cached or not. */
  private synchronized boolean canCacheNewValue(V value) {
    int newValueSize = mValueDescriptor.getSizeInBytes(value);
    return (newValueSize <= mMemoryCacheParams.maxCacheEntrySize) &&
        (getInUseCount() + 1 <= mMemoryCacheParams.maxCacheEntries) &&
        (getInUseSizeInBytes() + newValueSize <= mMemoryCacheParams.maxCacheSize);
  }

  /**
   * Gets the item with the given key, or null if there is no such item.
   *
   * <p> It is the caller's responsibility to close the returned reference once not needed anymore.
   */
  @Nullable
  public CloseableReference<V> get(final K key) {
    CloseableReference<V> clientRef = null;
    synchronized (this) {
      mExclusiveEntries.remove(key);
      Entry<K, V> entry = mCachedEntries.get(key);
      if (entry != null) {
        clientRef = newClientReference(entry);
      }
    }
    maybeUpdateCacheParams();
    maybeEvictEntries();
    return clientRef;
  }

  /** Creates a new reference for the client. */
  private synchronized CloseableReference<V> newClientReference(final Entry<K, V> entry) {
    increaseClientCount(entry);
    return CloseableReference.of(
        entry.valueRef.get(),
        new ResourceReleaser<V>() {
          @Override
          public void release(V unused) {
            releaseClientReference(entry);
          }
        });
  }

  /** Called when the client closes its reference. */
  private void releaseClientReference(final Entry<K, V> entry) {
    Preconditions.checkNotNull(entry);
    CloseableReference<V> oldRefToClose;
    synchronized (this) {
      decreaseClientCount(entry);
      maybeAddToExclusives(entry);
      oldRefToClose = referenceToClose(entry);
    }
    CloseableReference.closeSafely(oldRefToClose);
    maybeUpdateCacheParams();
    maybeEvictEntries();
  }

  /** Adds the entry to the exclusively owned queue if it is viable for eviction. */
  private synchronized void maybeAddToExclusives(Entry<K, V> entry) {
    if (!entry.isOrphan && entry.clientCount == 0) {
      mExclusiveEntries.put(entry.key, entry);
    }
  }

  /**
   * Removes all the items from the cache whose key matches the specified predicate.
   *
   * @param predicate returns true if an item with the given key should be removed
   * @return number of the items removed from the cache
   */
  public int removeAll(Predicate<K> predicate) {
    ArrayList<Entry<K, V>> oldEntries;
    synchronized (this) {
      mExclusiveEntries.removeAll(predicate);
      oldEntries = mCachedEntries.removeAll(predicate);
      makeOrphans(oldEntries);
    }
    maybeClose(oldEntries);
    maybeUpdateCacheParams();
    maybeEvictEntries();
    return oldEntries.size();
  }

  /** Removes all the items from the cache. */
  public void clear() {
    ArrayList<Entry<K, V>> oldEntries;
    synchronized (this) {
      mExclusiveEntries.clear();
      oldEntries = mCachedEntries.clear();
      makeOrphans(oldEntries);
    }
    maybeClose(oldEntries);
    maybeUpdateCacheParams();
  }

  /** Trims the cache according to the specified trimming strategy and the given trim type. */
  @Override
  public void trim(MemoryTrimType trimType) {
    ArrayList<Entry<K, V>> oldEntries;
    final double trimRatio = mCacheTrimStrategy.getTrimRatio(trimType);
    synchronized (this) {
      int targetCacheSize = (int) (mCachedEntries.getSizeInBytes() * (1 - trimRatio));
      int targetEvictionQueueSize = Math.max(0, targetCacheSize - getInUseSizeInBytes());
      oldEntries = trimExclusivelyOwnedEntries(Integer.MAX_VALUE, targetEvictionQueueSize);
      makeOrphans(oldEntries);
    }
    maybeClose(oldEntries);
    maybeUpdateCacheParams();
    maybeEvictEntries();
  }

  /**
   * Updates the cache params (constraints) if enough time has passed since the last update.
   */
  private synchronized void maybeUpdateCacheParams() {
    if (mLastCacheParamsCheck + PARAMS_INTERCHECK_INTERVAL_MS > SystemClock.elapsedRealtime()) {
      return;
    }
    mLastCacheParamsCheck = SystemClock.elapsedRealtime();
    mMemoryCacheParams = mMemoryCacheParamsSupplier.get();
  }

  /**
   * Removes the exclusively owned items until the cache constraints are met.
   *
   * <p> This method invokes the external {@link CloseableReference#close} method,
   * so it must not be called while holding the <code>this</code> lock.
   */
  private void maybeEvictEntries() {
    ArrayList<Entry<K, V>> oldEntries;
    synchronized (this) {
      int maxCount = Math.min(
          mMemoryCacheParams.maxEvictionQueueEntries,
          mMemoryCacheParams.maxCacheEntries - getInUseCount());
      int maxSize = Math.min(
          mMemoryCacheParams.maxEvictionQueueSize,
          mMemoryCacheParams.maxCacheSize - getInUseSizeInBytes());
      oldEntries = trimExclusivelyOwnedEntries(maxCount, maxSize);
      makeOrphans(oldEntries);
    }
    maybeClose(oldEntries);
  }

  /**
   * Removes the exclusively owned items until there is at most <code>count</code> of them
   * and they occupy no more than <code>size</code> bytes.
   *
   * <p> This method returns the removed items instead of actually closing them, so it is safe to
   * be called while holding the <code>this</code> lock.
   */
  @Nullable
  private synchronized ArrayList<Entry<K, V>> trimExclusivelyOwnedEntries(int count, int size) {
    count = Math.max(count, 0);
    size = Math.max(size, 0);
    // fast path without array allocation if no eviction is necessary
    if (mExclusiveEntries.getCount() <= count && mExclusiveEntries.getSizeInBytes() <= size) {
      return null;
    }
    ArrayList<Entry<K, V>> oldEntries = new ArrayList<>();
    while (mExclusiveEntries.getCount() > count || mExclusiveEntries.getSizeInBytes() > size) {
      K key = mExclusiveEntries.getFirstKey();
      mExclusiveEntries.remove(key);
      oldEntries.add(mCachedEntries.remove(key));
    }
    return oldEntries;
  }

  /**
   * Notifies the client that the cache no longer tracks the given items.
   *
   * <p> This method invokes the external {@link CloseableReference#close} method,
   * so it must not be called while holding the <code>this</code> lock.
   */
  private void maybeClose(@Nullable ArrayList<Entry<K, V>> oldEntries) {
    if (oldEntries != null) {
      for (Entry<K, V> oldEntry : oldEntries) {
        CloseableReference.closeSafely(referenceToClose(oldEntry));
      }
    }
  }

  /** Marks the given entries as orphans. */
  private synchronized void makeOrphans(@Nullable ArrayList<Entry<K, V>> oldEntries) {
    if (oldEntries != null) {
      for (Entry<K, V> oldEntry : oldEntries) {
        makeOrphan(oldEntry);
      }
    }
  }

  /** Marks the entry as orphan. */
  private synchronized void makeOrphan(Entry<K, V> entry) {
    Preconditions.checkNotNull(entry);
    Preconditions.checkState(!entry.isOrphan);
    entry.isOrphan = true;
  }

  /** Increases the entry's client count. */
  private synchronized void increaseClientCount(Entry<K, V> entry) {
    Preconditions.checkNotNull(entry);
    Preconditions.checkState(!entry.isOrphan);
    entry.clientCount++;
  }

  /** Decreases the entry's client count. */
  private synchronized void decreaseClientCount(Entry<K, V> entry) {
    Preconditions.checkNotNull(entry);
    Preconditions.checkState(entry.clientCount > 0);
    entry.clientCount--;
  }

  /** Returns the value reference of the entry if it should be closed, null otherwise. */
  @Nullable
  private synchronized CloseableReference<V> referenceToClose(Entry<K, V> entry) {
    Preconditions.checkNotNull(entry);
    return (entry.isOrphan && entry.clientCount == 0) ? entry.valueRef : null;
  }

  /** Gets the total number of all currently cached items. */
  public synchronized int getCount() {
    return mCachedEntries.getCount();
  }

  /** Gets the total size in bytes of all currently cached items. */
  public synchronized int getSizeInBytes() {
    return mCachedEntries.getSizeInBytes();
  }

  /** Gets the number of the cached items that are used by at least one client. */
  public synchronized int getInUseCount() {
    return mCachedEntries.getCount() - mExclusiveEntries.getCount();
  }

  /** Gets the total size in bytes of the cached items that are used by at least one client. */
  public synchronized int getInUseSizeInBytes() {
    return mCachedEntries.getSizeInBytes() - mExclusiveEntries.getSizeInBytes();
  }

  /** Gets the number of the exclusively owned items. */
  public synchronized int getEvictionQueueCount() {
    return mExclusiveEntries.getCount();
  }

  /** Gets the total size in bytes of the exclusively owned items. */
  public synchronized int getEvictionQueueSizeInBytes() {
    return mExclusiveEntries.getSizeInBytes();
  }
}

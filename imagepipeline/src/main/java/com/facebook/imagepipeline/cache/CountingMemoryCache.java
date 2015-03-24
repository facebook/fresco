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

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import android.os.SystemClock;

import com.facebook.common.internal.Lists;
import com.facebook.common.internal.Maps;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.Sets;
import com.facebook.common.internal.Supplier;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.logging.FLog;
import com.facebook.common.memory.MemoryTrimType;
import com.facebook.common.memory.MemoryTrimmable;
import com.facebook.common.references.CloseableReference;

import com.android.internal.util.Predicate;

/**
 * Layer of memory cache stack responsible for managing ownership of cached objects.
 * This layer maintains size counters and per-entry state which permits distinguishing between
 * objects that are currently being used by clients and the ones that are exclusively owned by the
 * cache.
 *
 * <p> This layer is responsible for lru eviction strategy and maintaining size boundaries of
 * cached values.
 *
 * @param <K> Cache key type
 * @param <V> Value type
 * @param <S> Type of extra object used by lookup algorithm
 */
@ThreadSafe
public class CountingMemoryCache<K, V, S> implements MemoryTrimmable {

  private static final Class<?> TAG = CountingMemoryCache.class;

  /**
   * CacheEntry is internal representation for key value pair stored by the cache.
   * Important detail of this class is how it implements equals method - keys are compared using
   * their equals method. To compare values == operator is used.
   */
  @VisibleForTesting
  static class CacheEntry<K, V> {
    public final K key;
    public final CloseableReference<V> value;

    private CacheEntry(final K key, final CloseableReference<V> value) {
      this.key = Preconditions.checkNotNull(key);
      this.value = Preconditions.checkNotNull(value);
    }

    @Override
    public int hashCode() {
      return key.hashCode() ^ System.identityHashCode(value);
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }

      if (!(o instanceof CacheEntry)) {
        return false;
      }

      CacheEntry<K, V> other = (CacheEntry<K, V>) o;
      return key.equals(other.key) && value == other.value;
    }

    @VisibleForTesting
    static <K, V> CacheEntry<K, V> of(final K key, final CloseableReference<V> value) {
      return new CacheEntry<K, V>(key, value);
    }
  }

  /**
   * Whether this cache entry is in use, and by whom - clients, cache, both, or neither.
   */
  @VisibleForTesting
  static enum EntryState {
    /**
     * Entry is not cached. The cache has no reference to the entry at all.
     */
    NOT_CACHED,

    /**
     * At least one client of the cache is using this value, and it has not been evicted.
     */
    SHARED,

    /**
     * The entry is in the cache, and has not been evicted, but is not in use by any client.
     */
    EXCLUSIVELY_OWNED,

    /**
     * The entry has been evicted, but at least one client is still references it.
     */
    ORPHAN,
  }

  /**
   * how often cache checks for new cache configuration
   */
  @VisibleForTesting
  static final long PARAMS_INTERCHECK_INTERVAL_MS = TimeUnit.MINUTES.toMillis(5);

  @GuardedBy("this")
  private final MemoryCacheIndex<K, V, S> mMemoryCacheIndex;

  @GuardedBy("this")
  @VisibleForTesting
  final LinkedHashSet<CacheEntry<K, V>> mEvictionQueue;
  @GuardedBy("this")
  private int mEvictionQueueSize;

  /**
   * maps all cached values to their in-use counters
   */
  @GuardedBy("this")
  @VisibleForTesting
  final Map<CacheEntry<K, V>, AtomicInteger> mCachedEntries;
  @GuardedBy("this")
  private int mCachedValuesSize;

  /**
   * maps all values evicted from cache which still are referenced by at least one client
   * to their in-use counters
   */
  @GuardedBy("this")
  @VisibleForTesting
  final Map<CacheEntry<K, V>, AtomicInteger> mOrphans;

  /**
   * Callback used to obtain size of cached values
   */
  private final ValueInfoCallback<V> mValueInfoCallback;

  private final CacheTrimStrategy mCacheTrimStrategy;

  /**
   * cache size boundaries
   */
  @GuardedBy("this")
  protected MemoryCacheParams mMemoryCacheParams;
  @GuardedBy("this")
  private long mLastCacheParamsCheck;
  private final Supplier<MemoryCacheParams> mMemoryCacheParamsSupplier;

  public CountingMemoryCache(
      MemoryCacheIndex<K, V, S> memoryCacheIndex,
      ValueInfoCallback<V> valueInfoCallback,
      CacheTrimStrategy cacheTrimStrategy,
      Supplier<MemoryCacheParams> memoryCacheParamsSupplier) {
    mMemoryCacheIndex = memoryCacheIndex;
    mEvictionQueue = Sets.newLinkedHashSet();
    mCachedEntries = Maps.newHashMap();
    mOrphans = Maps.newHashMap();
    mValueInfoCallback = valueInfoCallback;
    mCacheTrimStrategy = cacheTrimStrategy;
    mMemoryCacheParamsSupplier = memoryCacheParamsSupplier;

    mMemoryCacheParams = mMemoryCacheParamsSupplier.get();
    mLastCacheParamsCheck = SystemClock.elapsedRealtime();
  }

  /**
   * Determines current state of given entry. This method also checks state preconditions.
   * @param entry
   * @return state of given KeyValuePair
   */
  @VisibleForTesting
  synchronized EntryState getEntryState(final CacheEntry<K, V> entry) {
    Preconditions.checkNotNull(entry);
    if (mCachedEntries.containsKey(entry)) {
      if (mEvictionQueue.contains(entry)) {
        Preconditions.checkState(mCachedEntries.get(entry).get() == 0);
        return EntryState.EXCLUSIVELY_OWNED;
      } else {
        Preconditions.checkState(mCachedEntries.get(entry).get() > 0);
        return EntryState.SHARED;
      }
    } else {
      Preconditions.checkState(!mEvictionQueue.contains(entry));
      if (mOrphans.containsKey(entry)) {
        Preconditions.checkState(mOrphans.get(entry).get() > 0);
        return EntryState.ORPHAN;
      } else {
        return EntryState.NOT_CACHED;
      }
    }
  }

  /**
   * Caches given key value pair. After this method returns, caller should use returned instance
   * of CloseableReference, which depending on implementation might or might not be the same as
   * the one provided by caller. Caller is responsible for calling release method to notify
   * cache that cached value won't be used anymore. This allows the cache to maintain value's
   * in-use count.
   */
  public CloseableReference<V> cache(final K key, final CloseableReference<V> value) {
    Preconditions.checkNotNull(key);
    Preconditions.checkNotNull(value);

    maybeUpdateCacheParams();
    /**
     * We might be required to remove old entry associated with the same key.
     */
    CloseableReference<V> removedValue = null;
    CacheEntry<K, V> newCacheEntry = null;

    synchronized (this) {
      if (!canCacheNewValue(value)) {
        return null;
      }

      newCacheEntry = CacheEntry.of(key, value.clone());
      removedValue = handleIndexRegistration(key, newCacheEntry.value);
      putInCachedEntries(newCacheEntry);
      increaseUsageCount(newCacheEntry);
    }

    if (removedValue != null) {
      removedValue.close();
    }

    maybeEvictEntries();

    return newCacheEntry.value;
  }

  /**
   * Searches cache for value corresponding to given key. Additional strategy parameter is used
   * by cache index to return the best available value for given key (cache can contain multiple
   * values for the same key).
   *
   * <p> After this method returns non-null value, caller is responsible for calling release
   * method. This allows cache to maintain value's in-use count.
   */
  public CloseableReference<V> get(final K key, @Nullable S strategy) {
    CloseableReference<V> cachedValue;
    synchronized (this) {
      cachedValue = mMemoryCacheIndex.lookupValue(key, strategy);
      if (cachedValue != null) {
        final CacheEntry<K, V> cacheEntry = CacheEntry.of(key, cachedValue);
        final EntryState entryState = getEntryState(cacheEntry);
        switch (entryState) {
          case SHARED:
            increaseUsageCount(cacheEntry);
            break;

          case EXCLUSIVELY_OWNED:
            removeFromEvictionQueue(cacheEntry);
            increaseUsageCount(cacheEntry);
            break;

          case ORPHAN:
          case NOT_CACHED:
          default:
            Preconditions.checkState(
                false,
                "MemoryCacheIndex returned value in invalid state: %s",
                entryState);
        }
      }
    }
    maybeUpdateCacheParams();
    return cachedValue;
  }

  /**
   * Decreases in-use count associated with given value. Only entries with count 0 can
   * be evicted from the cache.
   */
  public void release(final K key, final CloseableReference<V> value) {
    /**
     * We might need to close value if corresponding KeyValuePair is an orphan.
     */
    boolean shouldCloseReference = false;

    synchronized (this) {
      final CacheEntry<K, V> cacheEntry = CacheEntry.of(key, value);
      final EntryState entryState = getEntryState(cacheEntry);

      switch (entryState) {
        case SHARED:
          decreaseUsageCount(cacheEntry);
          maybeAddToEvictionQueue(cacheEntry);
          break;

        case ORPHAN:
          shouldCloseReference = decreaseOrphansUsageCountAndMaybeRemove(cacheEntry);
          break;

        case EXCLUSIVELY_OWNED:
        case NOT_CACHED:
        default:
          Preconditions.checkState(
              false,
              "Released value is not in valid state: %s",
              entryState);
      }
    }

    if (shouldCloseReference) {
      value.close();
    }
  }

  /**
   * Removes all entries from the cache. Exclusively owned references are closed, and shared values
   * becomes orphans.
   */
  public void clear() {
    Collection<CloseableReference<V>> evictedEntries;

    synchronized (this) {
      evictedEntries = trimEvictionQueueTo(0, 0);
      for (CacheEntry<K, V> cacheEntry : Lists.newArrayList(mCachedEntries.keySet())) {
        moveFromCachedEntriesToOrphans(cacheEntry);
        mMemoryCacheIndex.removeEntry(cacheEntry.key, cacheEntry.value);
      }
    }

    for (CloseableReference<V> reference : evictedEntries) {
      reference.close();
    }
  }

  /**
   * Removes all entries from the cache whose keys match the specified predicate
   * @param match returns true if a key should be removed
   * @return number of entries that were evicted from the cache
   */
  public long removeAll(Predicate<K> match) {
    long numEvictedEntries = 0;
    List<CacheEntry<K, V>> matchingEntriesFromEvictionQueue;
    synchronized (this) {
      matchingEntriesFromEvictionQueue = getMatchingEntriesFromEvictionQueue(match);
      numEvictedEntries += matchingEntriesFromEvictionQueue.size();
      for (CacheEntry<K, V> cacheEntry : matchingEntriesFromEvictionQueue) {
        removeFromEvictionQueue(cacheEntry);
        removeFromCachedEntries(cacheEntry);
        mMemoryCacheIndex.removeEntry(cacheEntry.key, cacheEntry.value);
      }

      List<CacheEntry<K, V>> matchingCachedEntries = getMatchingCachedEntries(match);
      numEvictedEntries += matchingCachedEntries.size();
      for (CacheEntry<K, V> cacheEntry : matchingCachedEntries) {
        moveFromCachedEntriesToOrphans(cacheEntry);
        mMemoryCacheIndex.removeEntry(cacheEntry.key, cacheEntry.value);
      }
    }

    for (CacheEntry<K, V> cacheEntry : matchingEntriesFromEvictionQueue) {
      cacheEntry.value.close();
    }

    return numEvictedEntries;
  }

  private List<CacheEntry<K, V>> getMatchingEntriesFromEvictionQueue(Predicate<K> match) {
    List<CacheEntry<K, V>> matchingEntries = Lists.newArrayList();
    synchronized (this) {
      for (CacheEntry<K, V> cacheEntry : mEvictionQueue) {
        if (match.apply(cacheEntry.key)) {
          matchingEntries.add(cacheEntry);
        }
      }
    }
    return matchingEntries;
  }

  private List<CacheEntry<K, V>> getMatchingCachedEntries(Predicate<K> match) {
    List<CacheEntry<K, V>> matchingEntries = Lists.newArrayList();
    synchronized (this) {
      for (CacheEntry<K, V> cacheEntry : mCachedEntries.keySet()) {
        if (match.apply(cacheEntry.key)) {
          matchingEntries.add(cacheEntry);
        }
      }
    }
    return matchingEntries;
  }

  /**
   * Removes all exclusively owned values from the cache. Corresponding closeable references are
   * closed.
   */
  public void clearEvictionQueue() {
    Collection<CloseableReference<V>> evictedEntries = trimEvictionQueueTo(0, 0);
    for (CloseableReference<V> reference : evictedEntries) {
      reference.close();
    }
  }

  public void trimCacheTo(int maxCount, int maxSize) {
    Collection<CloseableReference<V>> evictedEntries;
    synchronized (this) {
      // Only max(maxCount - entries in use, 0) may stay in eviction queue
      final int maxLruCount =
          Math.max(maxCount - (mCachedEntries.size() - mEvictionQueue.size()), 0);
      // Entries in eviction queue can occupy only max(maxSize - size of entries in use, 0) bytes
      final int maxLruSize =
          Math.max(maxSize - (mCachedValuesSize - mEvictionQueueSize), 0);
      evictedEntries = trimEvictionQueueTo(maxLruCount, maxLruSize);
    }
    for (CloseableReference<V> reference : evictedEntries) {
      reference.close();
    }
  }

  @Override
  public void trim(MemoryTrimType trimType) {
    FLog.v(TAG, "Trimming cache, trim type %s", String.valueOf(trimType));
    mCacheTrimStrategy.trimCache(this, trimType);
  }

  /**
   * @return number of cached entries
   */
  public synchronized int getCount() {
    return mCachedEntries.size();
  }

  /**
   * @return total size in bytes of cached entries
   */
  public synchronized int getSizeInBytes() {
    return mCachedValuesSize;
  }

  /**
   * @return number of cached entries eligible for eviction
   */
  public synchronized int getEvictionQueueCount() {
    return mEvictionQueue.size();
  }

  /**
   * @return total size in bytes of entries tha are eligible for eviction
   */
  public synchronized int getEvictionQueueSizeInBytes() {
    return mEvictionQueueSize;
  }

  /**
   * Check cache params to determine if the cache is capable of storing another value.
   */
  private synchronized boolean canCacheNewValue(final CloseableReference<V> value) {
    Preconditions.checkState(mCachedValuesSize >= mEvictionQueueSize);
    Preconditions.checkState(mCachedEntries.size() >= mEvictionQueue.size());

    final long newValueSize = mValueInfoCallback.getSizeInBytes(value.get());
    final long sharedEntries = mCachedEntries.size() - mEvictionQueue.size();
    final long sharedEntriesByteSize = mCachedValuesSize - mEvictionQueueSize;
    return (newValueSize <= mMemoryCacheParams.maxCacheEntrySize) &&
        (sharedEntries < mMemoryCacheParams.maxCacheEntries) &&
        (sharedEntriesByteSize + newValueSize <= mMemoryCacheParams.maxCacheSize);
  }

  /**
   * If enough time has passed, updates mMemoryCacheParams. In such case some entries might be
   * evicted from the cache, so this method should be called only if calling thread does not
   * hold "this" lock.
   */
  @VisibleForTesting
  void maybeUpdateCacheParams() {
    synchronized (this) {
      if (mLastCacheParamsCheck + PARAMS_INTERCHECK_INTERVAL_MS > SystemClock.elapsedRealtime()) {
        return;
      }

      mLastCacheParamsCheck = SystemClock.elapsedRealtime();
      mMemoryCacheParams = mMemoryCacheParamsSupplier.get();
    }
    maybeEvictEntries();
  }

  /**
   * Evicts exclusively owned entries that do not fit cache limits.
   *
   * <p> There are multiple limits to respect here:
   * <ul>
   *   <ol> The total numbers of items in the cache </ol>
   *   <ol> The number of items in the cache that are 'exclusively owned' - not referenced outside
   *   the cache. These are stored in the eviction queue </ol>
   *   <ol> Total number of bytes in the cache </ol>
   *   <ol> Bytes in the eviction queue </ol>
   * </ul>
   * <p> This method closes corresponding references, so it should be not called when "this" lock is
   * acquired.
   */
  @VisibleForTesting
  void maybeEvictEntries() {
    Collection<CloseableReference<V>> evictedValues;

    synchronized (this) {

      final int allowedEvictionQueueCount = newEvictionQueueLimit(
          mCachedEntries.size(),
          mMemoryCacheParams.maxCacheEntries,
          mEvictionQueue.size(),
          mMemoryCacheParams.maxEvictionQueueEntries);

      final long allowedEvictionQueueBytes = newEvictionQueueLimit(
          mCachedValuesSize,
          mMemoryCacheParams.maxCacheSize,
          mEvictionQueueSize,
          mMemoryCacheParams.maxEvictionQueueSize);

      evictedValues = trimEvictionQueueTo(
          allowedEvictionQueueCount,
          allowedEvictionQueueBytes);
    }

    for (CloseableReference<V> evictedValue : evictedValues) {
      evictedValue.close();
    }
  }

  /**
   * Removes exclusively owned values from the cache until there is at most count of them
   * and they occupy no more than size bytes.
   */
  private synchronized Collection<CloseableReference<V>> trimEvictionQueueTo(
      int count,
      long size) {
    Preconditions.checkArgument(count >= 0);
    Preconditions.checkArgument(size >= 0);

    List<CloseableReference<V>> evictedValues = Lists.newArrayList();
    while (mEvictionQueue.size() > count || mEvictionQueueSize > size) {
      CacheEntry<K, V> cacheEntry = mEvictionQueue.iterator().next();
      evictedValues.add(cacheEntry.value);
      removeFromEvictionQueue(cacheEntry);
      removeFromCachedEntries(cacheEntry);
      mMemoryCacheIndex.removeEntry(cacheEntry.key, cacheEntry.value);
    }
    return evictedValues;
  }

  /**
   * Increments in-use counter for given ReferenceWrapper.
   */
  private synchronized void increaseUsageCount(final CacheEntry<K, V> cacheEntry) {
    AtomicInteger counter = mCachedEntries.get(cacheEntry);
    Preconditions.checkNotNull(counter);
    counter.incrementAndGet();
  }

  /**
   * Decrements in-use counter fot given ReferenceWrapper. Only pairs with counter 0 might be
   * evicted from the cache.
   */
  private synchronized void decreaseUsageCount(final CacheEntry<K, V> cacheEntry) {
    AtomicInteger counter = mCachedEntries.get(cacheEntry);
    Preconditions.checkNotNull(counter);
    Preconditions.checkState(counter.get() > 0);
    counter.decrementAndGet();
  }

  /**
   * Decreases in-use count for an orphan. If count reaches 0, the orphan is removed.
   */
  private synchronized boolean decreaseOrphansUsageCountAndMaybeRemove(
      final CacheEntry<K, V> cacheEntry) {
    AtomicInteger counter = mOrphans.get(cacheEntry);
    Preconditions.checkNotNull(counter);
    Preconditions.checkState(counter.get() > 0);
    if (counter.decrementAndGet() == 0) {
      mOrphans.remove(cacheEntry);
      return true;
    }
    return false;
  }

  /**
   * Queues given ReferenceWrapper in mEvictionQueue if its in-use count is equal to zero.
   */
  private synchronized void maybeAddToEvictionQueue(final CacheEntry<K, V> cacheEntry) {
    AtomicInteger counter = mCachedEntries.get(cacheEntry);
    Preconditions.checkNotNull(counter);
    Preconditions.checkArgument(!mEvictionQueue.contains(cacheEntry));
    if (counter.get() == 0) {
      mEvictionQueueSize += mValueInfoCallback.getSizeInBytes(cacheEntry.value.get());
      mEvictionQueue.add(cacheEntry);
    }
  }

  private synchronized void removeFromEvictionQueue(final CacheEntry<K, V> cacheEntry) {
    final long valueSize = mValueInfoCallback.getSizeInBytes(cacheEntry.value.get());
    Preconditions.checkState(mEvictionQueueSize >= valueSize);

    Preconditions.checkNotNull(mEvictionQueue.remove(cacheEntry));
    mEvictionQueueSize -= valueSize;
  }

  private synchronized void putInCachedEntries(final CacheEntry<K, V> cacheEntry) {
    Preconditions.checkState(!mCachedEntries.containsKey(cacheEntry));
    mCachedValuesSize += mValueInfoCallback.getSizeInBytes(cacheEntry.value.get());
    mCachedEntries.put(cacheEntry, new AtomicInteger());
  }

  private synchronized void removeFromCachedEntries(final CacheEntry<K, V> cacheEntry) {
    final long valueSize = mValueInfoCallback.getSizeInBytes(cacheEntry.value.get());
    Preconditions.checkState(mCachedValuesSize >= valueSize);
    Preconditions.checkNotNull(mCachedEntries.remove(cacheEntry));
    mCachedValuesSize -= valueSize;
  }

  private synchronized void moveFromCachedEntriesToOrphans(
      final CacheEntry<K, V> cacheEntry) {
    final AtomicInteger counter = mCachedEntries.get(cacheEntry);
    removeFromCachedEntries(cacheEntry);
    mOrphans.put(cacheEntry, counter);
  }

  /**
   * Registers given CloseableReference with MemoryCacheIndex. MemoryCacheIndex might request other
   * element to be removed from the cache. This method handles cache removal but lets the caller
   * close corresponding closeable reference - it should be done outside of "this" lock
   */
  private synchronized CloseableReference<V> handleIndexRegistration(
      final K key,
      final CloseableReference<V> newReference) {
    final CloseableReference<V> removedReference =
        mMemoryCacheIndex.addEntry(key, newReference);
    if (removedReference == null) {
      return null;
    }

    final CacheEntry<K, V> removedWrappedReference = CacheEntry.of(key, removedReference);
    final EntryState state = getEntryState(removedWrappedReference);

    switch (state) {
      case SHARED:
        moveFromCachedEntriesToOrphans(removedWrappedReference);
        return null;

      case EXCLUSIVELY_OWNED:
        removeFromEvictionQueue(removedWrappedReference);
        removeFromCachedEntries(removedWrappedReference);
        return removedReference;

      case ORPHAN:
      case NOT_CACHED:
      default:
        Preconditions.checkState(
            false,
            "MemoryCacheIndex returned value in invalid state %s",
            state);
        return null;
    }
  }

  /**
   * Helper method for computing eviction queue limit.
   */
  private static int newEvictionQueueLimit(
      int currentTotal,
      int maxTotal,
      int currentEvictionQueue,
      int maxEvictionQueue) {
    final int trimNeeded = Math.max(0, currentTotal - maxTotal);
    final int afterTrim = Math.max(0, currentEvictionQueue - trimNeeded);
    return Math.min(maxEvictionQueue, afterTrim);
  }

  /**
   * Interface used by the cache to query information about cached values.
   */
  public static interface ValueInfoCallback<V> {
    long getSizeInBytes(final V value);
  }

  /**
   * Interface used to specify cache trim behavior.
   */
  public static interface CacheTrimStrategy {
    void trimCache(CountingMemoryCache<?, ?, ?> cache, MemoryTrimType trimType);
  }
}

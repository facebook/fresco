/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache;

import android.graphics.Bitmap;
import android.os.SystemClock;
import com.facebook.cache.common.HasDebugData;
import com.facebook.common.internal.Objects;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.Predicate;
import com.facebook.common.internal.Supplier;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.memory.MemoryTrimType;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.references.ResourceReleaser;
import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Layer of memory cache stack responsible for managing eviction of the the cached items.
 *
 * <p>This layer is responsible for LRU eviction strategy and for maintaining the size boundaries of
 * the cached items.
 *
 * <p>Only the exclusively owned elements, i.e. the elements not referenced by any client, can be
 * evicted.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
@ThreadSafe
public class LruCountingMemoryCache<K, V>
    implements CountingMemoryCache<K, V>, MemoryCache<K, V>, HasDebugData {

  private final @Nullable EntryStateObserver<K> mEntryStateObserver;

  // Contains the items that are not being used by any client and are hence viable for eviction.
  @GuardedBy("this")
  @VisibleForTesting
  final CountingLruMap<K, Entry<K, V>> mExclusiveEntries;

  // Contains all the cached items including the exclusively owned ones.
  @GuardedBy("this")
  @VisibleForTesting
  final CountingLruMap<K, Entry<K, V>> mCachedEntries;

  @GuardedBy("this")
  @VisibleForTesting
  final Map<Bitmap, Object> mOtherEntries = new WeakHashMap<>();

  private final ValueDescriptor<V> mValueDescriptor;

  private final CacheTrimStrategy mCacheTrimStrategy;

  // Cache size constraints.
  private final Supplier<MemoryCacheParams> mMemoryCacheParamsSupplier;

  @GuardedBy("this")
  protected MemoryCacheParams mMemoryCacheParams;

  @GuardedBy("this")
  private long mLastCacheParamsCheck;

  public LruCountingMemoryCache(
      ValueDescriptor<V> valueDescriptor,
      CacheTrimStrategy cacheTrimStrategy,
      Supplier<MemoryCacheParams> memoryCacheParamsSupplier,
      @Nullable EntryStateObserver<K> entryStateObserver) {
    mValueDescriptor = valueDescriptor;
    mExclusiveEntries = new CountingLruMap<>(wrapValueDescriptor(valueDescriptor));
    mCachedEntries = new CountingLruMap<>(wrapValueDescriptor(valueDescriptor));
    mCacheTrimStrategy = cacheTrimStrategy;
    mMemoryCacheParamsSupplier = memoryCacheParamsSupplier;
    mMemoryCacheParams = mMemoryCacheParamsSupplier.get();
    mLastCacheParamsCheck = SystemClock.uptimeMillis();
    mEntryStateObserver = entryStateObserver;
  }

  private ValueDescriptor<Entry<K, V>> wrapValueDescriptor(
      final ValueDescriptor<V> evictableValueDescriptor) {
    return new ValueDescriptor<Entry<K, V>>() {
      @Override
      public int getSizeInBytes(Entry<K, V> entry) {
        return evictableValueDescriptor.getSizeInBytes(entry.valueRef.get());
      }
    };
  }

  /**
   * Caches the given key-value pair.
   *
   * <p>Important: the client should use the returned reference instead of the original one. It is
   * the caller's responsibility to close the returned reference once not needed anymore.
   *
   * @return the new reference to be used, null if the value cannot be cached
   */
  public CloseableReference<V> cache(final K key, final CloseableReference<V> valueRef) {
    return cache(key, valueRef, mEntryStateObserver);
  }

  /**
   * Caches the given key-value pair.
   *
   * <p>Important: the client should use the returned reference instead of the original one. It is
   * the caller's responsibility to close the returned reference once not needed anymore.
   *
   * @return the new reference to be used, null if the value cannot be cached
   */
  @Override
  public @Nullable CloseableReference<V> cache(
      final K key, final CloseableReference<V> valueRef, final EntryStateObserver<K> observer) {
    Preconditions.checkNotNull(key);
    Preconditions.checkNotNull(valueRef);

    maybeUpdateCacheParams();

    Entry<K, V> oldExclusive;
    CloseableReference<V> oldRefToClose = null;
    CloseableReference<V> clientRef = null;
    synchronized (this) {
      // remove the old item (if any) as it is stale now
      oldExclusive = mExclusiveEntries.remove(key);
      Entry<K, V> oldEntry = mCachedEntries.remove(key);
      if (oldEntry != null) {
        makeOrphan(oldEntry);
        oldRefToClose = referenceToClose(oldEntry);
      }

      if (canCacheNewValue(valueRef.get())) {
        Entry<K, V> newEntry = Entry.of(key, valueRef, observer);
        mCachedEntries.put(key, newEntry);
        clientRef = newClientReference(newEntry);
      }
    }
    CloseableReference.closeSafely(oldRefToClose);
    maybeNotifyExclusiveEntryRemoval(oldExclusive);

    maybeEvictEntries();
    return clientRef;
  }

  /** Checks the cache constraints to determine whether the new value can be cached or not. */
  private synchronized boolean canCacheNewValue(V value) {
    int newValueSize = mValueDescriptor.getSizeInBytes(value);
    return (newValueSize <= mMemoryCacheParams.maxCacheEntrySize)
        && (getInUseCount() <= mMemoryCacheParams.maxCacheEntries - 1)
        && (getInUseSizeInBytes() <= mMemoryCacheParams.maxCacheSize - newValueSize);
  }

  /**
   * Gets the item with the given key, or null if there is no such item.
   *
   * <p>It is the caller's responsibility to close the returned reference once not needed anymore.
   */
  @Nullable
  public CloseableReference<V> get(final K key) {
    Preconditions.checkNotNull(key);
    Entry<K, V> oldExclusive;
    CloseableReference<V> clientRef = null;
    synchronized (this) {
      oldExclusive = mExclusiveEntries.remove(key);
      Entry<K, V> entry = mCachedEntries.get(key);
      if (entry != null) {
        clientRef = newClientReference(entry);
      }
    }
    maybeNotifyExclusiveEntryRemoval(oldExclusive);
    maybeUpdateCacheParams();
    maybeEvictEntries();
    return clientRef;
  }

  /**
   * Probes whether the object corresponding to the key is in the cache. Note that the act of
   * probing touches the item (if present in cache), thus changing its LRU timestamp.
   */
  @Override
  public void probe(final K key) {
    Preconditions.checkNotNull(key);
    Entry<K, V> oldExclusive;
    synchronized (this) {
      oldExclusive = mExclusiveEntries.remove(key);
      if (oldExclusive != null) {
        mExclusiveEntries.put(key, oldExclusive);
      }
    }
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
    boolean isExclusiveAdded;
    CloseableReference<V> oldRefToClose;
    synchronized (this) {
      decreaseClientCount(entry);
      isExclusiveAdded = maybeAddToExclusives(entry);
      oldRefToClose = referenceToClose(entry);
    }
    CloseableReference.closeSafely(oldRefToClose);
    maybeNotifyExclusiveEntryInsertion(isExclusiveAdded ? entry : null);
    maybeUpdateCacheParams();
    maybeEvictEntries();
  }

  /** Adds the entry to the exclusively owned queue if it is viable for eviction. */
  private synchronized boolean maybeAddToExclusives(Entry<K, V> entry) {
    if (!entry.isOrphan && entry.clientCount == 0) {
      mExclusiveEntries.put(entry.key, entry);
      return true;
    }
    return false;
  }

  /**
   * Gets the value with the given key to be reused, or null if there is no such value.
   *
   * <p>The item can be reused only if it is exclusively owned by the cache.
   */
  @Override
  @Nullable
  public CloseableReference<V> reuse(K key) {
    Preconditions.checkNotNull(key);
    CloseableReference<V> clientRef = null;
    boolean removed = false;
    Entry<K, V> oldExclusive = null;
    synchronized (this) {
      oldExclusive = mExclusiveEntries.remove(key);
      if (oldExclusive != null) {
        Entry<K, V> entry = mCachedEntries.remove(key);
        Preconditions.checkNotNull(entry);
        Preconditions.checkState(entry.clientCount == 0);
        // optimization: instead of cloning and then closing the original reference,
        // we just do a move
        clientRef = entry.valueRef;
        removed = true;
      }
    }
    if (removed) {
      maybeNotifyExclusiveEntryRemoval(oldExclusive);
    }
    return clientRef;
  }

  /**
   * Removes all the items from the cache whose key matches the specified predicate.
   *
   * @param predicate returns true if an item with the given key should be removed
   * @return number of the items removed from the cache
   */
  public int removeAll(Predicate<K> predicate) {
    ArrayList<Entry<K, V>> oldExclusives;
    ArrayList<Entry<K, V>> oldEntries;
    synchronized (this) {
      oldExclusives = mExclusiveEntries.removeAll(predicate);
      oldEntries = mCachedEntries.removeAll(predicate);
      makeOrphans(oldEntries);
    }
    maybeClose(oldEntries);
    maybeNotifyExclusiveEntryRemoval(oldExclusives);
    maybeUpdateCacheParams();
    maybeEvictEntries();
    return oldEntries.size();
  }

  /** Removes all the items from the cache. */
  @Override
  public void clear() {
    ArrayList<Entry<K, V>> oldExclusives;
    ArrayList<Entry<K, V>> oldEntries;
    synchronized (this) {
      oldExclusives = mExclusiveEntries.clear();
      oldEntries = mCachedEntries.clear();
      makeOrphans(oldEntries);
    }
    maybeClose(oldEntries);
    maybeNotifyExclusiveEntryRemoval(oldExclusives);
    maybeUpdateCacheParams();
  }

  /**
   * Check if any items from the cache whose key matches the specified predicate.
   *
   * @param predicate returns true if an item with the given key matches
   * @return true is any items matches from the cache
   */
  @Override
  public synchronized boolean contains(Predicate<K> predicate) {
    return !mCachedEntries.getMatchingEntries(predicate).isEmpty();
  }

  /**
   * Check if an item with the given cache key is currently in the cache.
   *
   * @param key returns true if an item with the given key matches
   * @return true is any items matches from the cache
   */
  @Override
  public synchronized boolean contains(K key) {
    return mCachedEntries.contains(key);
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
    maybeNotifyExclusiveEntryRemoval(oldEntries);
    maybeUpdateCacheParams();
    maybeEvictEntries();
  }

  /** Updates the cache params (constraints) if enough time has passed since the last update. */
  private synchronized void maybeUpdateCacheParams() {
    if (mLastCacheParamsCheck + mMemoryCacheParams.paramsCheckIntervalMs
        > SystemClock.uptimeMillis()) {
      return;
    }
    mLastCacheParamsCheck = SystemClock.uptimeMillis();
    mMemoryCacheParams = mMemoryCacheParamsSupplier.get();
  }

  public MemoryCacheParams getMemoryCacheParams() {
    return mMemoryCacheParams;
  }

  @Override
  public CountingLruMap<K, Entry<K, V>> getCachedEntries() {
    return mCachedEntries;
  }

  @Override
  public Map<Bitmap, Object> getOtherEntries() {
    return mOtherEntries;
  }

  /**
   * Removes the exclusively owned items until the cache constraints are met.
   *
   * <p>This method invokes the external {@link CloseableReference#close} method, so it must not be
   * called while holding the <code>this</code> lock.
   */
  @Override
  public void maybeEvictEntries() {
    ArrayList<Entry<K, V>> oldEntries;
    synchronized (this) {
      int maxCount =
          Math.min(
              mMemoryCacheParams.maxEvictionQueueEntries,
              mMemoryCacheParams.maxCacheEntries - getInUseCount());
      int maxSize =
          Math.min(
              mMemoryCacheParams.maxEvictionQueueSize,
              mMemoryCacheParams.maxCacheSize - getInUseSizeInBytes());
      oldEntries = trimExclusivelyOwnedEntries(maxCount, maxSize);
      makeOrphans(oldEntries);
    }
    maybeClose(oldEntries);
    maybeNotifyExclusiveEntryRemoval(oldEntries);
  }

  /**
   * Removes the exclusively owned items until there is at most <code>count</code> of them and they
   * occupy no more than <code>size</code> bytes.
   *
   * <p>This method returns the removed items instead of actually closing them, so it is safe to be
   * called while holding the <code>this</code> lock.
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
   * <p>This method invokes the external {@link CloseableReference#close} method, so it must not be
   * called while holding the <code>this</code> lock.
   */
  private void maybeClose(@Nullable ArrayList<Entry<K, V>> oldEntries) {
    if (oldEntries != null) {
      for (Entry<K, V> oldEntry : oldEntries) {
        CloseableReference.closeSafely(referenceToClose(oldEntry));
      }
    }
  }

  private void maybeNotifyExclusiveEntryRemoval(@Nullable ArrayList<Entry<K, V>> entries) {
    if (entries != null) {
      for (Entry<K, V> entry : entries) {
        maybeNotifyExclusiveEntryRemoval(entry);
      }
    }
  }

  private static <K, V> void maybeNotifyExclusiveEntryRemoval(@Nullable Entry<K, V> entry) {
    if (entry != null && entry.observer != null) {
      entry.observer.onExclusivityChanged(entry.key, false);
    }
  }

  private static <K, V> void maybeNotifyExclusiveEntryInsertion(@Nullable Entry<K, V> entry) {
    if (entry != null && entry.observer != null) {
      entry.observer.onExclusivityChanged(entry.key, true);
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
  @Override
  public synchronized int getCount() {
    return mCachedEntries.getCount();
  }

  /** Gets the total size in bytes of all currently cached items. */
  @Override
  public synchronized int getSizeInBytes() {
    return mCachedEntries.getSizeInBytes();
  }

  /** Gets the number of the cached items that are used by at least one client. */
  public synchronized int getInUseCount() {
    return mCachedEntries.getCount() - mExclusiveEntries.getCount();
  }

  /** Gets the total size in bytes of the cached items that are used by at least one client. */
  @Override
  public synchronized int getInUseSizeInBytes() {
    return mCachedEntries.getSizeInBytes() - mExclusiveEntries.getSizeInBytes();
  }

  /** Gets the number of the exclusively owned items. */
  @Override
  public synchronized int getEvictionQueueCount() {
    return mExclusiveEntries.getCount();
  }

  /** Gets the total size in bytes of the exclusively owned items. */
  @Override
  public synchronized int getEvictionQueueSizeInBytes() {
    return mExclusiveEntries.getSizeInBytes();
  }

  @Override
  public @Nullable String getDebugData() {
    return Objects.toStringHelper("CountingMemoryCache")
        .add("cached_entries_count:", mCachedEntries.getCount())
        .add("cached_entries_size_bytes", mCachedEntries.getSizeInBytes())
        .add("exclusive_entries_count", mExclusiveEntries.getCount())
        .add("exclusive_entries_size_bytes", mExclusiveEntries.getSizeInBytes())
        .toString();
  }
}

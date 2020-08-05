/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache;

import android.graphics.Bitmap;
import android.os.SystemClock;
import com.facebook.common.internal.Objects;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.Predicate;
import com.facebook.common.internal.Supplier;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.logging.FLog;
import com.facebook.common.memory.MemoryTrimType;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.references.ResourceReleaser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Layer of memory cache stack responsible for managing eviction of the the cached items.
 *
 * <p>This layer is responsible for Adaptive Replacement Cache (ARC) eviction strategy and for
 * maintaining the size boundaries of the cached items.
 *
 * <p>Only the exclusively owned elements, i.e. the elements not referenced by any client, can be
 * evicted.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
@ThreadSafe
public abstract class AbstractAdaptiveCountingMemoryCache<K, V>
    implements CountingMemoryCache<K, V> {
  private static final String TAG = "AbstractArcCountingMemoryCache";

  // Contains the least frequently used items out of all the iterms in the cache that are not being
  // used by any client and are hence viable for eviction.
  @GuardedBy("this")
  @VisibleForTesting
  final CountingLruMap<K, Entry<K, V>> mLeastFrequentlyUsedExclusiveEntries;

  // Contains the most frequently used items out of all the items in the cache that are not being
  // used by any client and are hence viable for eviction.
  @GuardedBy("this")
  @VisibleForTesting
  final CountingLruMap<K, Entry<K, V>> mMostFrequentlyUsedExclusiveEntries;

  // Contains all the cached items including the exclusively owned ones.
  @GuardedBy("this")
  @VisibleForTesting
  final CountingLruMap<K, Entry<K, V>> mCachedEntries;

  private final ValueDescriptor<V> mValueDescriptor;

  private final CacheTrimStrategy mCacheTrimStrategy;

  // Cache size constraints.
  private final Supplier<MemoryCacheParams> mMemoryCacheParamsSupplier;

  // 0 < mLFUFractionPromil < 1000
  // The mLFUFractionPromil/1000 is the the percentage of the cache allocated for the least
  // frequently used values. The rest of the cache, which is the (1 - mLFUFractionPromil/1000) is
  // allocated for the most frequently used values.
  @GuardedBy("this")
  @VisibleForTesting
  int mLFUFractionPromil;
  // default LFU Fraction
  static final int DEFAULT_LFU_FRACTION_PROMIL = 500;

  // These constants are used to define the smallest LFU/MFU fraction sizes.
  // considering the cache is partioned [Cache] = [..LFU..|..MFU..]: LFU between 0 and N, MFU
  // between N and 1000.
  static final int TOTAL_PROMIL = 1000;
  @VisibleForTesting static final int MIN_FRACTION_PROMIL = 100;
  static final int MAX_FRACTION_PROMIL = TOTAL_PROMIL - MIN_FRACTION_PROMIL;

  // Threshold is used to determine if the value is most (or least) frequently used.
  // if the accessCount is less or equals to threshold, the value is considered as a least
  // frequently used. Otherwise, most frequently used.
  private final int mFrequentlyUsedThreshold;

  // The learning rate for adapting the cache partitions; this determines how much we
  // increase/decrease the fraction
  @GuardedBy("this")
  @VisibleForTesting
  final int mAdaptiveRatePromil;
  // defualt adaptive rate
  static final int DEFAULT_ADAPTIVE_RATE_PROMIL = 10;

  // Tracks the most recently evicted keys from the least frequently used cache.
  @GuardedBy("this")
  @VisibleForTesting
  final IntMapArrayList<K> mLeastFrequentlyUsedKeysGhostList;

  // Tracks the most recently evicted keys from the most frequently used cache.
  @GuardedBy("this")
  @VisibleForTesting
  final ArrayList<K> mMostFrequentlyUsedKeysGhostList;

  // The maximum size of the ghost lists.
  @GuardedBy("this")
  @VisibleForTesting
  final int mGhostListMaxSize;

  enum ArrayListType {
    LFU, // Least Frequently Used
    MFU // Most Frequently Used
  }

  @GuardedBy("this")
  protected MemoryCacheParams mMemoryCacheParams;

  @GuardedBy("this")
  private long mLastCacheParamsCheck;

  public AbstractAdaptiveCountingMemoryCache(
      Supplier<MemoryCacheParams> memoryCacheParamsSupplier,
      CacheTrimStrategy cacheTrimStrategy,
      ValueDescriptor<V> valueDescriptor,
      int adaptiveRatePromil,
      int frequentlyUsedThreshold,
      int ghostListMaxSize,
      int lfuFractionPromil) {
    FLog.d(TAG, "Create Adaptive Replacement Cache");
    mValueDescriptor = valueDescriptor;
    mLeastFrequentlyUsedExclusiveEntries =
        new CountingLruMap<>(wrapValueDescriptor(valueDescriptor));
    mMostFrequentlyUsedExclusiveEntries =
        new CountingLruMap<>(wrapValueDescriptor(valueDescriptor));
    mCachedEntries = new CountingLruMap<>(wrapValueDescriptor(valueDescriptor));
    mCacheTrimStrategy = cacheTrimStrategy;
    mMemoryCacheParamsSupplier = memoryCacheParamsSupplier;
    mMemoryCacheParams = mMemoryCacheParamsSupplier.get();
    mLastCacheParamsCheck = SystemClock.uptimeMillis();
    mFrequentlyUsedThreshold = frequentlyUsedThreshold;
    mGhostListMaxSize = ghostListMaxSize;
    mLeastFrequentlyUsedKeysGhostList = new IntMapArrayList<>(mGhostListMaxSize);
    mMostFrequentlyUsedKeysGhostList = new ArrayList<>(mGhostListMaxSize);
    if (lfuFractionPromil < MIN_FRACTION_PROMIL || lfuFractionPromil > MAX_FRACTION_PROMIL) {
      mLFUFractionPromil = DEFAULT_LFU_FRACTION_PROMIL;
      logIllegalLfuFraction();
    } else {
      mLFUFractionPromil = lfuFractionPromil;
    }
    if (adaptiveRatePromil <= 0 || adaptiveRatePromil >= TOTAL_PROMIL) {
      mAdaptiveRatePromil = DEFAULT_ADAPTIVE_RATE_PROMIL;
      logIllegalAdaptiveRate();
    } else {
      mAdaptiveRatePromil = adaptiveRatePromil;
    }
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
    return cache(key, valueRef, null);
  }

  /**
   * Caches the given key-value pair.
   *
   * <p>Important: the client should use the returned reference instead of the original one. It is
   * the caller's responsibility to close the returned reference once not needed anymore.
   *
   * @return the new reference to be used, null if the value cannot be cached
   */
  public @Nullable CloseableReference<V> cache(
      final K key, final CloseableReference<V> valueRef, final EntryStateObserver<K> observer) {
    Preconditions.checkNotNull(key);
    Preconditions.checkNotNull(valueRef);

    maybeUpdateCacheParams();

    final Entry<K, V> oldLFUExclusive;
    final Entry<K, V> oldMFUExclusive;
    CloseableReference<V> oldRefToClose = null;
    CloseableReference<V> clientRef = null;
    synchronized (this) {
      // remove the old item (if any) as it is stale now
      oldLFUExclusive = mLeastFrequentlyUsedExclusiveEntries.remove(key);
      oldMFUExclusive = mMostFrequentlyUsedExclusiveEntries.remove(key);
      final boolean notPresentInBoth = oldLFUExclusive == null || oldMFUExclusive == null;
      Preconditions.checkState(notPresentInBoth);
      Entry<K, V> oldEntry = mCachedEntries.remove(key);
      if (oldEntry != null) {
        makeOrphan(oldEntry);
        oldRefToClose = referenceToClose(oldEntry);
      }

      if (canCacheNewValue(valueRef.get())) {
        Entry<K, V> newEntry = Entry.of(key, valueRef, observer);
        // check if the key was recently evicted and restore its state
        Integer storedAccessCount = mLeastFrequentlyUsedKeysGhostList.getValue(key);
        newEntry.accessCount = storedAccessCount != null ? storedAccessCount : 0;
        mCachedEntries.put(key, newEntry);
        clientRef = newClientReference(newEntry);
      }
    }
    CloseableReference.closeSafely(oldRefToClose);
    maybeNotifyExclusiveEntryRemoval(oldLFUExclusive, oldMFUExclusive);

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
    final Entry<K, V> oldLFUExclusive;
    final Entry<K, V> oldMFUExclusive;
    CloseableReference<V> clientRef = null;
    synchronized (this) {
      oldLFUExclusive = mLeastFrequentlyUsedExclusiveEntries.remove(key);
      oldMFUExclusive = mMostFrequentlyUsedExclusiveEntries.remove(key);
      Entry<K, V> entry = mCachedEntries.get(key);
      if (entry != null) {
        clientRef = newClientReference(entry);
      } else {
        maybeUpdateCacheFraction(key);
      }
    }
    maybeNotifyExclusiveEntryRemoval(oldLFUExclusive, oldMFUExclusive);
    maybeUpdateCacheParams();
    maybeEvictEntries();
    return clientRef;
  }

  /**
   * Probes whether the object corresponding to the key is in the cache. Note that the act of
   * probing touches the item (if present in cache), thus changing its LRU timestamp.
   */
  public void probe(K key) {
    Preconditions.checkNotNull(key);
    Entry<K, V> oldExclusive;
    synchronized (this) {
      oldExclusive = mLeastFrequentlyUsedExclusiveEntries.remove(key);
      if (oldExclusive == null) {
        oldExclusive = mMostFrequentlyUsedExclusiveEntries.remove(key);
      }
      if (oldExclusive != null) {
        increaseAccessCount(oldExclusive);
        maybeAddToExclusives(oldExclusive);
      }
    }
  }

  /** Changes the relative size of LFU and MFU if necessary and updates the ghost lists. */
  private synchronized void maybeUpdateCacheFraction(K key) {
    if (mLeastFrequentlyUsedKeysGhostList.contains(key)) {
      if (mLFUFractionPromil + mAdaptiveRatePromil <= MAX_FRACTION_PROMIL) {
        // key was recently evicted from LFU, if we had had larger LFU, we would have hit the
        // cache
        mLFUFractionPromil += mAdaptiveRatePromil;
      }
      // update the key's accessCount and move it to the youngest position since it should have been
      // accessed by now.
      mLeastFrequentlyUsedKeysGhostList.increaseValueIfExists(key);
    } else if (mLFUFractionPromil - mAdaptiveRatePromil >= MIN_FRACTION_PROMIL
        && mMostFrequentlyUsedKeysGhostList.contains(key)) {
      // key was recently evicted from MFU, if we had had larger MFU, we would have hit the
      // cache
      mLFUFractionPromil -= mAdaptiveRatePromil;
    }
  }

  /** Creates a new reference for the client. */
  private synchronized CloseableReference<V> newClientReference(final Entry<K, V> entry) {
    increaseCounters(entry);
    return CloseableReference.of(
        entry.valueRef.get(),
        new ResourceReleaser<V>() {
          @Override
          public void release(V unused) {
            releaseClientReference(entry);
          }
        });
  }

  /**
   * Adds a key to the ghost list and removes the eldest elemenet if the size of the list is bigger
   * than mGhostListMaxSize
   */
  private synchronized void addElementToGhostList(
      K key, int accessCount, ArrayListType evictionType) {
    if (evictionType == ArrayListType.LFU) {
      mLeastFrequentlyUsedKeysGhostList.addPair(key, accessCount);
    } else {
      if (mMostFrequentlyUsedKeysGhostList.size() == mGhostListMaxSize) {
        mMostFrequentlyUsedKeysGhostList.remove(0);
      }
      mMostFrequentlyUsedKeysGhostList.add(key);
    }
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
      if (entry.accessCount > mFrequentlyUsedThreshold) {
        mMostFrequentlyUsedExclusiveEntries.put(entry.key, entry);
      } else {
        mLeastFrequentlyUsedExclusiveEntries.put(entry.key, entry);
      }
      return true;
    }
    return false;
  }

  /**
   * Gets the value with the given key to be reused, or null if there is no such value.
   *
   * <p>The item can be reused only if it is exclusively owned by the cache.
   */
  @Nullable
  public CloseableReference<V> reuse(K key) {
    Preconditions.checkNotNull(key);
    CloseableReference<V> clientRef = null;
    boolean removed = false;
    Entry<K, V> oldExclusive = null;
    synchronized (this) {
      oldExclusive = mLeastFrequentlyUsedExclusiveEntries.remove(key);
      if (oldExclusive == null) {
        oldExclusive = mMostFrequentlyUsedExclusiveEntries.remove(key);
      }
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
    ArrayList<Entry<K, V>> oldLFUExclusives;
    ArrayList<Entry<K, V>> oldMFUExclusives;
    ArrayList<Entry<K, V>> oldEntries;
    synchronized (this) {
      oldLFUExclusives = mLeastFrequentlyUsedExclusiveEntries.removeAll(predicate);
      oldMFUExclusives = mMostFrequentlyUsedExclusiveEntries.removeAll(predicate);
      oldEntries = mCachedEntries.removeAll(predicate);
      makeOrphans(oldEntries);
    }
    maybeClose(oldEntries);
    maybeNotifyExclusiveEntriesRemoval(oldLFUExclusives, oldMFUExclusives);
    maybeUpdateCacheParams();
    maybeEvictEntries();
    return oldEntries.size();
  }

  /** Removes all the items from the cache. */
  public void clear() {
    ArrayList<Entry<K, V>> oldLFUExclusives;
    ArrayList<Entry<K, V>> oldMFUExclusives;
    ArrayList<Entry<K, V>> oldEntries;
    synchronized (this) {
      oldLFUExclusives = mLeastFrequentlyUsedExclusiveEntries.clear();
      oldMFUExclusives = mMostFrequentlyUsedExclusiveEntries.clear();
      oldEntries = mCachedEntries.clear();
      makeOrphans(oldEntries);
    }
    maybeClose(oldEntries);
    maybeNotifyExclusiveEntriesRemoval(oldLFUExclusives, oldMFUExclusives);
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

  /**
   * Trims the cache according to the specified trimming strategy and the given trim type. We first
   * trim the LFU cache, if we need to trim more, we continue to trim the MFU cache.
   */
  @Override
  public void trim(MemoryTrimType trimType) {
    ArrayList<Entry<K, V>> oldLFUEntries;
    ArrayList<Entry<K, V>> oldMFUEntries;
    final double trimRatio = mCacheTrimStrategy.getTrimRatio(trimType);
    synchronized (this) {
      final int targetCacheSize = (int) (mCachedEntries.getSizeInBytes() * (1 - trimRatio));
      final int targetEvictionQueueSize = Math.max(0, targetCacheSize - getInUseSizeInBytes());
      int MFUTargetEvictionQueueSize = mMostFrequentlyUsedExclusiveEntries.getSizeInBytes();
      int LFUTargetEvictionQueueSize =
          Math.max(0, targetEvictionQueueSize - MFUTargetEvictionQueueSize);
      if (targetEvictionQueueSize <= MFUTargetEvictionQueueSize) {
        MFUTargetEvictionQueueSize = targetEvictionQueueSize;
        LFUTargetEvictionQueueSize = 0;
      }
      oldLFUEntries =
          trimExclusivelyOwnedEntries(
              Integer.MAX_VALUE,
              LFUTargetEvictionQueueSize,
              mLeastFrequentlyUsedExclusiveEntries,
              ArrayListType.LFU);
      oldMFUEntries =
          trimExclusivelyOwnedEntries(
              Integer.MAX_VALUE,
              MFUTargetEvictionQueueSize,
              mMostFrequentlyUsedExclusiveEntries,
              ArrayListType.MFU);
      makeOrphans(oldLFUEntries, oldMFUEntries);
    }
    maybeClose(oldLFUEntries, oldMFUEntries);
    maybeNotifyExclusiveEntriesRemoval(oldLFUEntries, oldMFUEntries);
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

  /**
   * Removes the exclusively owned items until the cache constraints are met.
   *
   * <p>This method invokes the external {@link CloseableReference#close} method, so it must not be
   * called while holding the <code>this</code> lock.
   */
  @Override
  public void maybeEvictEntries() {
    ArrayList<Entry<K, V>> oldLFUEntries;
    ArrayList<Entry<K, V>> oldMFUEntries;
    synchronized (this) {
      int maxCount =
          Math.min(
              mMemoryCacheParams.maxEvictionQueueEntries,
              mMemoryCacheParams.maxCacheEntries - getInUseCount());
      int maxSize =
          Math.min(
              mMemoryCacheParams.maxEvictionQueueSize,
              mMemoryCacheParams.maxCacheSize - getInUseSizeInBytes());
      int LFUMaxCount = (int) ((long) maxCount * mLFUFractionPromil / TOTAL_PROMIL);
      int LFUMaxSize = (int) ((long) maxSize * mLFUFractionPromil / TOTAL_PROMIL);
      oldLFUEntries =
          trimExclusivelyOwnedEntries(
              LFUMaxCount, LFUMaxSize, mLeastFrequentlyUsedExclusiveEntries, ArrayListType.LFU);
      oldMFUEntries =
          trimExclusivelyOwnedEntries(
              maxCount - LFUMaxCount,
              maxSize - LFUMaxSize,
              mMostFrequentlyUsedExclusiveEntries,
              ArrayListType.MFU);
      makeOrphans(oldLFUEntries, oldMFUEntries);
    }
    maybeClose(oldLFUEntries, oldMFUEntries);
    maybeNotifyExclusiveEntriesRemoval(oldLFUEntries, oldMFUEntries);
  }

  /**
   * Removes the exclusively owned, least recently used items until there is at most <code>count
   * </code> of them and they occupy no more than <code>size</code> bytes.
   *
   * <p>This method returns the removed least frequently used items instead of actually closing
   * them, so it is safe to be called while holding the <code>this</code> lock.
   */
  @Nullable
  private synchronized ArrayList<Entry<K, V>> trimExclusivelyOwnedEntries(
      int count,
      int size,
      CountingLruMap<K, Entry<K, V>> ExclusixeEntries,
      ArrayListType evictionType) {
    count = Math.max(count, 0);
    size = Math.max(size, 0);
    // fast path without array allocation if no eviction is necessary
    if (ExclusixeEntries.getCount() <= count && ExclusixeEntries.getSizeInBytes() <= size) {
      return null;
    }
    ArrayList<Entry<K, V>> oldEntries = new ArrayList<>();
    while (ExclusixeEntries.getCount() > count || ExclusixeEntries.getSizeInBytes() > size) {
      K key = ExclusixeEntries.getFirstKey();
      addElementToGhostList(
          key, Preconditions.checkNotNull(ExclusixeEntries.get(key)).accessCount, evictionType);
      ExclusixeEntries.remove(key);
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
  private void maybeClose(
      @Nullable ArrayList<Entry<K, V>> oldEntries1, @Nullable ArrayList<Entry<K, V>> oldEntries2) {
    maybeClose(oldEntries1);
    maybeClose(oldEntries2);
  }

  private void maybeClose(@Nullable ArrayList<Entry<K, V>> oldEntries) {
    if (oldEntries != null) {
      for (Entry<K, V> oldEntry : oldEntries) {
        CloseableReference.closeSafely(referenceToClose(oldEntry));
      }
    }
  }

  private void maybeNotifyExclusiveEntriesRemoval(
      @Nullable ArrayList<Entry<K, V>> entries1, @Nullable ArrayList<Entry<K, V>> entries2) {
    maybeNotifyExclusiveEntryRemoval(entries1);
    maybeNotifyExclusiveEntryRemoval(entries2);
  }

  private void maybeNotifyExclusiveEntryRemoval(
      @Nullable Entry<K, V> entry1, @Nullable Entry<K, V> entry2) {
    maybeNotifyExclusiveEntryRemoval(entry1);
    maybeNotifyExclusiveEntryRemoval(entry2);
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
  private synchronized void makeOrphans(
      @Nullable ArrayList<Entry<K, V>> oldEntries1, @Nullable ArrayList<Entry<K, V>> oldEntries2) {
    makeOrphans(oldEntries1);
    makeOrphans(oldEntries2);
  }

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

  /** Increases the entry's client count and access count. */
  private synchronized void increaseCounters(Entry<K, V> entry) {
    Preconditions.checkNotNull(entry);
    Preconditions.checkState(!entry.isOrphan);
    entry.clientCount++;
    increaseAccessCount(entry);
  }

  /** Increases the entry's access count. */
  private synchronized void increaseAccessCount(Entry<K, V> entry) {
    Preconditions.checkNotNull(entry);
    Preconditions.checkState(!entry.isOrphan);
    entry.accessCount++;
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
    return mCachedEntries.getCount()
        - mLeastFrequentlyUsedExclusiveEntries.getCount()
        - mMostFrequentlyUsedExclusiveEntries.getCount();
  }

  /** Gets the total size in bytes of the cached items that are used by at least one client. */
  public synchronized int getInUseSizeInBytes() {
    return mCachedEntries.getSizeInBytes()
        - mLeastFrequentlyUsedExclusiveEntries.getSizeInBytes()
        - mMostFrequentlyUsedExclusiveEntries.getSizeInBytes();
  }

  /** Gets the number of the exclusively owned items. */
  public synchronized int getEvictionQueueCount() {
    return mLeastFrequentlyUsedExclusiveEntries.getCount()
        + mMostFrequentlyUsedExclusiveEntries.getCount();
  }

  /** Gets the total size in bytes of the exclusively owned items. */
  public synchronized int getEvictionQueueSizeInBytes() {
    return mLeastFrequentlyUsedExclusiveEntries.getSizeInBytes()
        + mMostFrequentlyUsedExclusiveEntries.getSizeInBytes();
  }

  public String reportData() {
    return Objects.toStringHelper("CountingMemoryCache")
        .add("cached_entries_count:", mCachedEntries.getCount())
        .add("exclusive_entries_count", getEvictionQueueCount())
        .toString();
  }

  /**
   * Mapping between E element and Integer value, implemented by using two bounded ArrayList. Each
   * pair is inserted, updated and removed together.
   */
  @VisibleForTesting
  class IntMapArrayList<E> {
    private final ArrayList<E> mFirstList;
    private final ArrayList<Integer> mSecondList;
    private final int mMaxCapacity;

    public IntMapArrayList(int maxCapacity) {
      mFirstList = new ArrayList<>(maxCapacity);
      mSecondList = new ArrayList<>(maxCapacity);
      mMaxCapacity = maxCapacity;
    }

    /**
     * When adding a new element, and the array size exceeds the mMaxCapccity, we remove the eldest
     * element and only then add the new element.
     */
    public void addPair(E element, Integer second) {
      if (mFirstList.size() == mMaxCapacity) {
        mFirstList.remove(0);
        mSecondList.remove(0);
      }
      mFirstList.add(element);
      mSecondList.add(second);
    }

    /** If the element exists in the List, increase its value and put it in the youngest position */
    public void increaseValueIfExists(E element) {
      final int index = mFirstList.indexOf(element);
      if (index < 0) {
        return;
      }
      Integer newInt = mSecondList.get(index) + 1;
      if (index == mMaxCapacity - 1) {
        mSecondList.set(mMaxCapacity - 1, newInt);
        return;
      }
      mFirstList.remove(index);
      mSecondList.remove(index);
      mFirstList.add(element);
      mSecondList.add(newInt);
    }

    /** returns the value mapped to the passed element if it exists. Otherwise, returns null. */
    @Nullable
    public Integer getValue(E element) {
      final int index = mFirstList.indexOf(element);
      if (index < 0) {
        return null;
      }
      return mSecondList.get(index);
    }

    public boolean contains(E element) {
      return mFirstList.contains(element);
    }

    public int size() {
      return mFirstList.size();
    }
  }

  protected abstract void logIllegalLfuFraction();

  protected abstract void logIllegalAdaptiveRate();

  @Override
  public CountingLruMap getCachedEntries() {
    return mCachedEntries;
  }

  @Override
  public Map<Bitmap, Object> getOtherEntries() {
    return Collections.emptyMap(); // TODO T66165815
  }

  /** The internal representation of a key-value pair stored by the cache. */
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
    @Nullable public final EntryStateObserver<K> observer;
    // The number of clients accessed this value while being in the cache.
    public int accessCount;

    private Entry(K key, CloseableReference<V> valueRef, @Nullable EntryStateObserver<K> observer) {
      this.key = Preconditions.checkNotNull(key);
      this.valueRef = Preconditions.checkNotNull(CloseableReference.cloneOrNull(valueRef));
      this.clientCount = 0;
      this.isOrphan = false;
      this.observer = observer;
      this.accessCount = 0;
    }

    /** Creates a new entry with the usage count and access count of 0. */
    @VisibleForTesting
    static <K, V> Entry<K, V> of(
        final K key,
        final CloseableReference<V> valueRef,
        final @Nullable EntryStateObserver<K> observer) {
      return new Entry<>(key, valueRef, observer);
    }
  }
}

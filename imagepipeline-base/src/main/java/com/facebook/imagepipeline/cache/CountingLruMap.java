/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.cache;

import com.facebook.common.internal.Predicate;
import com.facebook.common.internal.VisibleForTesting;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Map that keeps track of the elements order (according to the LRU policy) and their size.
 */
@ThreadSafe
public class CountingLruMap<K, V> {

  private final ValueDescriptor<V> mValueDescriptor;

  @GuardedBy("this")
  private final LinkedHashMap<K, V> mMap = new LinkedHashMap<>();
  @GuardedBy("this")
  private int mSizeInBytes = 0;

  public CountingLruMap(ValueDescriptor<V> valueDescriptor) {
    mValueDescriptor = valueDescriptor;
  }

  @VisibleForTesting
  synchronized ArrayList<K> getKeys() {
    return new ArrayList<>(mMap.keySet());
  }

  @VisibleForTesting
  synchronized ArrayList<V> getValues() {
    return new ArrayList<>(mMap.values());
  }

  /** Gets the count of the elements in the map. */
  public synchronized int getCount() {
    return mMap.size();
  }

  /** Gets the total size in bytes of the elements in the map. */
  public synchronized int getSizeInBytes() {
    return mSizeInBytes;
  }

  /** Gets the key of the first element in the map. */
  @Nullable
  public synchronized K getFirstKey() {
    return mMap.isEmpty() ? null : mMap.keySet().iterator().next();
  }

  /** Gets the all matching elements. */
  public synchronized ArrayList<LinkedHashMap.Entry<K, V>> getMatchingEntries(
      @Nullable Predicate<K> predicate) {
    ArrayList<LinkedHashMap.Entry<K, V>> matchingEntries = new ArrayList<>(mMap.entrySet().size());
    for (LinkedHashMap.Entry<K, V> entry : mMap.entrySet()) {
      if (predicate == null || predicate.apply(entry.getKey())) {
        matchingEntries.add(entry);
      }
    }
    return matchingEntries;
  }

  /** Returns whether the map contains an element with the given key.  */
  public synchronized boolean contains(K key) {
    return mMap.containsKey(key);
  }

  /** Gets the element from the map. */
  @Nullable
  public synchronized V get(K key) {
    return mMap.get(key);
  }

  /** Adds the element to the map, and removes the old element with the same key if any. */
  @Nullable
  public synchronized V put(K key, V value) {
    // We do remove and insert instead of just replace, in order to cause a structural change
    // to the map, as we always want the latest inserted element to be last in the queue.
    V oldValue = mMap.remove(key);
    mSizeInBytes -= getValueSizeInBytes(oldValue);
    mMap.put(key, value);
    mSizeInBytes += getValueSizeInBytes(value);
    return oldValue;
  }

  /** Removes the element from the map. */
  @Nullable
  public synchronized V remove(K key) {
      V oldValue = mMap.remove(key);
      mSizeInBytes -= getValueSizeInBytes(oldValue);
      return oldValue;
  }

  /** Removes all the matching elements from the map. */
  public synchronized ArrayList<V> removeAll(@Nullable Predicate<K> predicate) {
    ArrayList<V> oldValues = new ArrayList<>();
    Iterator<LinkedHashMap.Entry<K, V>> iterator = mMap.entrySet().iterator();
    while (iterator.hasNext()) {
      LinkedHashMap.Entry<K, V> entry = iterator.next();
      if (predicate == null || predicate.apply(entry.getKey())) {
        oldValues.add(entry.getValue());
        mSizeInBytes -= getValueSizeInBytes(entry.getValue());
        iterator.remove();
      }
    }
    return oldValues;
  }

  /** Clears the map. */
  public synchronized ArrayList<V> clear() {
    ArrayList<V> oldValues = new ArrayList<>(mMap.values());
    mMap.clear();
    mSizeInBytes = 0;
    return oldValues;
  }

  private int getValueSizeInBytes(V value) {
    return (value == null) ? 0 : mValueDescriptor.getSizeInBytes(value);
  }
}

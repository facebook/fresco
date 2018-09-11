/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.memory;

import android.util.SparseArray;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.infer.annotation.ThreadSafe;
import java.util.LinkedList;
import javax.annotation.Nullable;

/**
 * Map-like datastructure that allows to have more than one value per int key. Allows to remove a
 * value from LRU key by calling {@link #removeFromEnd()}
 */
@ThreadSafe
public class BucketMap<T> {
  protected final SparseArray<LinkedEntry<T>> mMap = new SparseArray<>();
  @VisibleForTesting @Nullable LinkedEntry<T> mHead;
  @VisibleForTesting @Nullable LinkedEntry<T> mTail;

  @VisibleForTesting
  static class LinkedEntry<I> {
    @Nullable LinkedEntry<I> prev;
    int key;
    LinkedList<I> value;
    @Nullable LinkedEntry<I> next;

    private LinkedEntry(
        @Nullable LinkedEntry<I> prev, int key, LinkedList<I> value, @Nullable LinkedEntry<I> next) {
      this.prev = prev;
      this.key = key;
      this.value = value;
      this.next = next;
    }

    @Override
    public String toString() {
      return "LinkedEntry(key: " + this.key + ")";
    }
  }

  /**
   * @param key
   * @return Retrieve an object that corresponds to the specified {@code key}
   * if present in the {@link BucketMap} or null otherwise
   */
  @Nullable
  public synchronized T acquire(int key) {
    LinkedEntry<T> bucket = mMap.get(key);
    if (bucket == null) {
      return null;
    }
    T result = bucket.value.pollFirst();
    moveToFront(bucket);

    return result;
  }

  /**
   * Associates the object with the specified key and puts it into the {@link BucketMap}.
   * Does not overwrite the previous object, if any.
   * @param key
   */
  public synchronized void release(int key, T value) {
    LinkedEntry<T> bucket = mMap.get(key);
    if (bucket == null) {
      bucket = new LinkedEntry<T>(null, key, new LinkedList<T>(), null);
      mMap.put(key, bucket);
    }

    bucket.value.addLast(value);

    moveToFront(bucket);
  }

  /**
   * @return number of objects contained in the {@link BucketMap}
   */
  @VisibleForTesting
  synchronized int valueCount() {
    int count = 0;
    LinkedEntry entry = mHead;
    while (entry != null) {
      if (entry.value != null) {
        count += entry.value.size();
      }
      entry = entry.next;
    }
    return count;
  }

  private synchronized void prune(LinkedEntry<T> bucket) {
    LinkedEntry<T> prev = bucket.prev;
    LinkedEntry<T> next = bucket.next;
    if (prev != null) {
      prev.next = next;
    }
    if (next != null) {
      next.prev = prev;
    }

    bucket.prev = null;
    bucket.next = null;

    if (bucket == mHead) {
      mHead = next;
    }

    if (bucket == mTail) {
      mTail = prev;
    }
  }

  private void moveToFront(LinkedEntry<T> bucket) {
    if (mHead == bucket) {
      return;
    }

    prune(bucket);

    if (mHead == null) {
      mHead = bucket;
      mTail = bucket;
      return;
    }

    bucket.next = mHead;
    mHead.prev = bucket;
    mHead = bucket;
  }

  @Nullable
  public synchronized T removeFromEnd() {
    LinkedEntry<T> last = mTail;
    if (last == null) {
      return null;
    }

    T value = last.value.pollLast();
    maybePrune(last);
    return value;
  }

  private void maybePrune(LinkedEntry<T> bucket) {
    if (bucket != null && bucket.value.isEmpty()) {
      prune(bucket);
      mMap.remove(bucket.key);
    }
  }
}

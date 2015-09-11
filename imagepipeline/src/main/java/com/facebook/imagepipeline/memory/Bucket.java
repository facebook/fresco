/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.memory;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import java.util.LinkedList;
import java.util.Queue;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.VisibleForTesting;

/**
 * The Bucket is a constituent class of {@link BasePool}. The pool maintains its free values
 * in a set of buckets, where each bucket represents a set of values of the same 'size'.
 * <p>
 * Each bucket maintains a freelist of values.
 * When the pool receives a {@link BasePool#get(Object)} request for a particular size, it finds the
 * appropriate bucket, and delegates the request to the bucket ({@link #get()}.
 * If the bucket's freelist is  non-empty, then one of the entries on the freelist is returned (and
 * removed from the freelist).
 * Similarly, when a value is released to the pool via a call to {@link BasePool#release(Object)},
 * the pool locates the appropriate bucket and returns the value to the bucket's freelist - see
 * ({@link #release(Object)}
 * <p>
 * The bucket also maintains the current number of items (from this bucket) that are "in use" i.e.
 * values that came from this bucket, but are now in use by the caller, and no longer on the
 * freelist.
 * The 'length' of the bucket is the number of values from this bucket that are currently in use
 * (mInUseCount), plus the size of the freeList. The maxLength of the bucket is that maximum length
 * that this bucket should grow to - and is used by the pool to determine whether values should
 * be released to the bucket ot freed.
 * @param <V> type of values to be 'stored' in the bucket
 */
@NotThreadSafe
@VisibleForTesting
class Bucket<V> {
  private static final String TAG = "com.facebook.imagepipeline.common.Bucket";

  public final int mItemSize; // size in bytes of items in this bucket
  public final int mMaxLength; // 'max' length for this bucket
  final Queue mFreeList; // the free list for this bucket, subclasses can vary type

  private int mInUseLength; // current number of entries 'in use' (i.e.) not in the free list

  /**
   * Constructs a new Bucket instance. The constructed bucket will have an empty freelist
   * @param itemSize size in bytes of each item in this bucket
   * @param maxLength max length for the bucket (used + free)
   * @param inUseLength current in-use-length for the bucket
   */
  public Bucket(int itemSize, int maxLength, int inUseLength) {
    Preconditions.checkState(itemSize > 0);
    Preconditions.checkState(maxLength >= 0);
    Preconditions.checkState(inUseLength >= 0);

    mItemSize = itemSize;
    mMaxLength = maxLength;
    mFreeList = new LinkedList();
    mInUseLength = inUseLength;
  }

  /**
   * Determines if the current length of the bucket (free + used) exceeds the max length
   * specified
   */
  public boolean isMaxLengthExceeded() {
    return (mInUseLength + getFreeListSize() > mMaxLength);
  }

  int getFreeListSize() {
    return mFreeList.size();
  }

  /**
   * Gets a free item if possible from the freelist. Returns null if the free list is empty
   * Updates the bucket inUse count
   * @return an item from the free list, if available
   */
  @Nullable
  public V get() {
    V value = pop();
    if (value != null) {
      mInUseLength++;
    }
    return value;
  }

  /**
   * Remove the first item (if any) from the freelist. Returns null if the free list is empty
   * Does not update the bucket inUse count
   * @return the first value (if any) from the free list
   */
  @Nullable
  public V pop() {
    return (V) mFreeList.poll();
  }

  /**
   * Increment the mInUseCount field.
   * Used by the pool to update the bucket info when a value was 'alloc'ed (because no free value
   * was available)
   */
  public void incrementInUseCount() {
    mInUseLength++;
  }

  /**
   * Releases a value to this bucket and decrements the inUse count
   * @param value the value to release
   */
  public void release(V value) {
    Preconditions.checkNotNull(value);
    Preconditions.checkState(mInUseLength > 0);
    mInUseLength--;
    addToFreeList(value);
  }

  void addToFreeList(V value) {
    mFreeList.add(value);
  }

  /**
   * Decrement the mInUseCount field.
   * Used by the pool to update the bucket info when a value was freed, instead of being returned
   * to the bucket's free list
   */
  public void decrementInUseCount() {
    Preconditions.checkState(mInUseLength > 0);
    mInUseLength--;
  }

  public int getInUseCount() {
    return mInUseLength;
  }
}

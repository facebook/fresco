/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.memory;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.NotThreadSafe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.annotation.SuppressLint;
import android.util.SparseArray;
import android.util.SparseIntArray;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.Sets;
import com.facebook.common.internal.Throwables;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.logging.FLog;
import com.facebook.common.memory.MemoryTrimType;
import com.facebook.common.memory.MemoryTrimmableRegistry;

/**
 * A base pool class that manages a pool of values (of type V). <p>
 * The pool is organized as a map. Each entry in the map is a free-list (modeled by a queue) of
 * entries for a given size.
 * Some pools have a fixed set of buckets (aka bucketized sizes), while others don't.
 * <p>
 * The pool supports two main operations:
 * <ul>
 *   <li> {@link #get(int)} - returns a value of size that's the same or larger than specified, hopefully
 *   from the pool; otherwise, this value is allocated (via the alloc function)</li>
 *   <li> {@link #release(V)} - releases a value to the pool</li>
 * </ul>
 * In addition, the pool subscribes to the {@link MemoryTrimmableRegistry}, and responds to
 * low-memory events (calls to trim). Some percent (perhaps all) of the values in the pool are then
 * released (via the underlying free function), and no longer belong to the pool.
 * <p>
 * Sizes
 * There are 3 different notions of sizes we consider here (not all of them may be relevant for
 * each use case).
 * <ul>
 *   <li>Logical size is simply the size of the value in terms appropriate for the value. For
 *   example, for byte arrays, the size is simply the length. For a bitmap, the size is just the
 *   number of pixels.</li>
 *   <li>Bucketed size typically represents one of discrete set of logical sizes - such that each
 *   bucketed size can accommodate a range of logical sizes. For example, for byte arrays, using
 *   sizes that are powers of 2 for bucketed sizes allows these byte arrays to support a number
 *   of logical sizes.</li>
 *   <li>Finally, Size-in-bytes is exactly that - the size of the value in bytes.</li>
 * </ul>
 * Logical Size and BucketedSize are both represented by the type parameter S, while size-in-bytes
 * is represented by an int.
 * <p>
 * Each concrete subclass of the pool must implement the following methods
 * <ul>
 *   <li>{@link #getBucketedSize(int)} - returns the bucketized size for the given request size</li>
 *   <li>{@link #getBucketedSizeForValue(Object)} - returns the bucketized size for a given
 *   value</li>
 *   <li>{@link #getSizeInBytes(int)} - gets the size in bytes for a given bucketized size</li>
 *   <li>{@link #alloc(int)} - allocates a value of given size</li>
 *   <li>{@link #free(Object)} - frees the value V</li>
 * Subclasses may optionally implement
 *   <li>{@link #onParamsChanged()} - called whenever this class determines to re-read the pool
 *   params</li>
 *   <li>{@link #isReusable(Object)} - used to determine if a value can be reused or must be
 *   freed</li>
 * </ul>
 * <p>
 * InUse values
 * The pool keeps track of values currently in use (in addition to the free values in the buckets).
 * This is maintained in an IdentityHashSet (using reference equality for the values). The in-use
 * set helps with accounting/book-keeping; we also use this during {@link #release(Object)} to avoid
 * messing with (freeing/reusing) values that are 'unknown' to the pool.
 * <p>
 * PoolParams
 * Pools are "configured" with a set of parameters (the PoolParams) supplied via a provider.
 * This set of parameters includes
 * <ul>
 *   <li> {@link PoolParams#maxSizeSoftCap}
 *   The size of a pool includes its used and free space. The maxSize setting
 *   for a pool is a soft cap on the overall size of the pool. A key point is that {@link #get(int)}
 *   requests will not fail because the max size has been exceeded (unless the underlying
 *   {@link #alloc(int)} function fails). However, the pool's free portion will be trimmed
 *   as much as possible so that the pool's size may fall below the max size. Note that when the
 *   free portion has fallen to zero, the pool may still be larger than its maxSizeSoftCap.
 *   On a {@link #release(Object)} request, the value will be 'freed' instead of being added to
 *   the free portion of the pool, if the pool exceeds its maxSizeSoftCap.
 *   The invariant we want to maintain - see {@link #ensurePoolSizeInvariant()} - is that the pool
 *   must be below the max size soft cap OR the free lists must be empty. </li>
 *   <li> {@link PoolParams#maxSizeHardCap}
 *   The hard cap is a stronger limit on the pool size. When this limit is reached, we first
 *   attempt to trim the pool. If the pool size is still over the hard, the
 *   {@link #get(int)} call will fail with a {@link PoolSizeViolationException} </li>
 *   <li> {@link PoolParams#bucketSizes}
 *   The pool can be configured with a set of 'sizes' - a bucket is created for each such size.
 *   Additionally, each bucket can have a a max-length specified, which is the sum of the used and
 *   free items in that bucket. As with the MaxSize parameter above, the maxLength here is a soft
 *   cap, in that it will not cause an exception on get; it simply controls the release path.
 *   If the BucketSizes parameter is null, then the pool will dynamically create buckets on demand.
 *   </li>
 * </ul>
 */
public abstract class BasePool<V> implements Pool<V> {
  private final Class<?> TAG = this.getClass();

  /**
   * The memory manager to register with
   */
  final MemoryTrimmableRegistry mMemoryTrimmableRegistry;

  /**
   * Provider for pool parameters
   */
  final PoolParams mPoolParams;

  /**
   * The buckets - representing different 'sizes'
   */
  @VisibleForTesting
  final SparseArray<Bucket<V>> mBuckets;

  /**
   * An Identity hash-set to keep track of values by reference equality
   */
  @VisibleForTesting
  final Set<V> mInUseValues;

  /**
   * Determines if new buckets can be created
   */
  private boolean mAllowNewBuckets;

  /**
   * tracks 'used space' - space allocated via the pool
   */
  @VisibleForTesting
  @GuardedBy("this")
  final Counter mUsed;

  /**
   * tracks 'free space' in the pool
   */
  @VisibleForTesting
  @GuardedBy("this")
  final Counter mFree;

  private final PoolStatsTracker mPoolStatsTracker;

  /**
   * Creates a new instance of the pool.
   * @param poolParams pool parameters
   * @param poolStatsTracker
   */
  public BasePool(
      MemoryTrimmableRegistry memoryTrimmableRegistry,
      PoolParams poolParams,
      PoolStatsTracker poolStatsTracker) {
    mMemoryTrimmableRegistry = Preconditions.checkNotNull(memoryTrimmableRegistry);
    mPoolParams = Preconditions.checkNotNull(poolParams);
    mPoolStatsTracker = Preconditions.checkNotNull(poolStatsTracker);

    // initialize the buckets
    mBuckets = new SparseArray<Bucket<V>>();
    initBuckets(new SparseIntArray(0));

    mInUseValues = Sets.newIdentityHashSet();

    mFree = new Counter();
    mUsed = new Counter();
  }

  /**
   * Finish pool initialization.
   */
  protected void initialize() {
    mMemoryTrimmableRegistry.registerMemoryTrimmable(this);
    mPoolStatsTracker.setBasePool(this);
  }

  /**
   * Gets a new 'value' from the pool, if available. Allocates a new value if necessary.
   * If we need to perform an allocation,
   *   - If the pool size exceeds the max-size soft cap, then we attempt to trim the free portion
   *     of the pool.
   *   - If the pool size exceeds the max-size hard-cap (after trimming), then we throw an
   *     {@link PoolSizeViolationException}
   * Bucket length constraints are not considered in this function
   * @param size the logical size to allocate
   * @return a new value
   * @throws InvalidSizeException
   */
  public V get(int size) {
    ensurePoolSizeInvariant();

    int bucketedSize = getBucketedSize(size);
    int sizeInBytes = -1;

    synchronized (this) {
      Bucket<V> bucket = getBucket(bucketedSize);

      if (bucket != null) {
        // find an existing value that we can reuse
        V value = bucket.get();
        if (value != null) {
          Preconditions.checkState(mInUseValues.add(value));

          // It is possible that we got a 'larger' value than we asked for.
          // lets recompute size in bytes here
          bucketedSize = getBucketedSizeForValue(value);
          sizeInBytes = getSizeInBytes(bucketedSize);
          mUsed.increment(sizeInBytes);
          mFree.decrement(sizeInBytes);
          mPoolStatsTracker.onValueReuse(sizeInBytes);
          logStats();
          if (FLog.isLoggable(FLog.VERBOSE)) {
            FLog.v(
                TAG,
                "get (reuse) (object, size) = (%x, %s)",
                System.identityHashCode(value),
                bucketedSize);
          }
          return value;
        }
        // fall through
      }
      // check to see if we can allocate a value of the given size without exceeding the hard cap
      sizeInBytes = getSizeInBytes(bucketedSize);
      if (!canAllocate(sizeInBytes)) {
        throw new PoolSizeViolationException(
            mPoolParams.maxSizeHardCap,
            mUsed.mNumBytes,
            mFree.mNumBytes,
            sizeInBytes);
      }

      // Optimistically assume that allocation succeeds - if it fails, we need to undo those changes
      mUsed.increment(sizeInBytes);
      if (bucket != null) {
        bucket.incrementInUseCount();
      }
    }

    V value = null;
    try {
      // allocate the value outside the synchronized block, because it can be pretty expensive
      // we could have done the allocation inside the synchronized block,
      // but that would have blocked out other operations on the pool
      value = alloc(bucketedSize);
    } catch (Throwable e) {
      // Assumption we made previously is not valid - allocation failed. We need to fix internal
      // counters.
      synchronized (this) {
        mUsed.decrement(sizeInBytes);
        Bucket<V> bucket = getBucket(bucketedSize);
        if (bucket != null) {
          bucket.decrementInUseCount();
        }
      }
      Throwables.propagateIfPossible(e);
    }

    // NOTE: We checked for hard caps earlier, and then did the alloc above. Now we need to
    // update state - but it is possible that a concurrent thread did a similar operation - with
    // the result being that we're now over the hard cap.
    // We are willing to live with that situation - especially since the trim call below should
    // be able to trim back memory usage.
    synchronized(this) {
      Preconditions.checkState(mInUseValues.add(value));
      // If we're over the pool's max size, try to trim the pool appropriately
      trimToSoftCap();
      mPoolStatsTracker.onAlloc(sizeInBytes);
      logStats();
      if (FLog.isLoggable(FLog.VERBOSE)) {
        FLog.v(
            TAG,
            "get (alloc) (object, size) = (%x, %s)",
            System.identityHashCode(value),
            bucketedSize);
      }
    }

    return value;
  }

  /**
   * Releases the given value to the pool.
   * In a few cases, the value is 'freed' instead of being released to the pool. If
   *   - the pool currently exceeds its max size OR
   *   - if the value does not map to a bucket that's currently maintained by the pool, OR
   *   - if the bucket for the value exceeds its maxLength, OR
   *   - if the value is not recognized by the pool
   *  then, the value is 'freed'.
   * @param value the value to release to the pool
   */
  @Override
  public void release(V value) {
    Preconditions.checkNotNull(value);

    final int bucketedSize = getBucketedSizeForValue(value);
    final int sizeInBytes = getSizeInBytes(bucketedSize);
    synchronized (this) {
      final Bucket<V> bucket = getBucket(bucketedSize);
      if (!mInUseValues.remove(value)) {
        // This value was not 'known' to the pool (i.e.) allocated via the pool.
        // Something is going wrong, so let's free the value and report soft error.
        FLog.e(
            TAG,
            "release (free, value unrecognized) (object, size) = (%x, %s)",
            System.identityHashCode(value),
            bucketedSize);
        free(value);
        mPoolStatsTracker.onFree(sizeInBytes);
      } else {
        // free the value, if
        //  - pool exceeds maxSize
        //  - there is no bucket for this value
        //  - there is a bucket for this value, but it has exceeded its maxLength
        //  - the value is not reusable
        // If no bucket was found for the value, simply free it
        // We should free the value if no bucket is found, or if the bucket length cap is exceeded.
        // However, if the pool max size softcap is exceeded, it may not always be best to free
        // *this* value.
        if (bucket == null ||
            bucket.isMaxLengthExceeded() ||
            isMaxSizeSoftCapExceeded() ||
            !isReusable(value)) {
          if (bucket != null) {
            bucket.decrementInUseCount();
          }

          if (FLog.isLoggable(FLog.VERBOSE)) {
            FLog.v(
                TAG,
                "release (free) (object, size) = (%x, %s)",
                System.identityHashCode(value),
                bucketedSize);
          }
          free(value);
          mUsed.decrement(sizeInBytes);
          mPoolStatsTracker.onFree(sizeInBytes);
        } else {
          bucket.release(value);
          mFree.increment(sizeInBytes);
          mUsed.decrement(sizeInBytes);
          mPoolStatsTracker.onValueRelease(sizeInBytes);
          if (FLog.isLoggable(FLog.VERBOSE)) {
            FLog.v(
                TAG,
                "release (reuse) (object, size) = (%x, %s)",
                System.identityHashCode(value),
                bucketedSize);
          }
        }
      }
      logStats();
    }
  }

  /**
   * Trims the pool in response to low-memory states (invoked from MemoryManager)
   * For now, we'll do the simplest thing, and simply clear out the entire pool. We may consider
   * more sophisticated approaches later.
   * In other words, we ignore the memoryTrimType parameter
   * @param memoryTrimType the kind of trimming we want to perform
   */
  public void trim(MemoryTrimType memoryTrimType) {
    trimToNothing();
  }

  /**
   * Allocates a new 'value' with the given size
   * @param bucketedSize the logical size to allocate
   * @return a new value
   */
  protected abstract V alloc(int bucketedSize);

  /**
   * Frees the 'value'
   * @param value the value to free
   */
  @VisibleForTesting
  protected abstract void free(V value);

  /**
   * Gets the bucketed size (typically something the same or larger than the requested size)
   * @param requestSize the logical request size
   * @return the 'bucketed' size
   * @throws InvalidSizeException, if the size of the value doesn't match the pool's constraints
   */
  protected abstract int getBucketedSize(int requestSize);

  /**
   * Gets the bucketed size of the value
   * @param value the value
   * @return bucketed size of the value
   * @throws InvalidSizeException, if the size of the value doesn't match the pool's constraints
   * @throws InvalidValueException, if the value is invalid
   */
  protected abstract int getBucketedSizeForValue(V value);

  /**
   * Gets the size in bytes for the given bucketed size
   * @param bucketedSize the bucketed size
   * @return size in bytes
   */
  protected abstract int getSizeInBytes(int bucketedSize);

  /**
   * The pool parameters may have changed. Subclasses can override this to update any state they
   * were maintaining
   */
  protected void onParamsChanged() {
  }

  /**
   * Determines if the supplied value is 'reusable'.
   * This is called during {@link #release(Object)}, and determines if the value can be added
   * to the freelists of the pool (for future reuse), or must be released right away.
   * Subclasses can override this to provide custom implementations
   * @param value the value to test for reusability
   * @return true if the value is reusable
   */
  protected boolean isReusable(V value) {
    Preconditions.checkNotNull(value);
    return true;
  }

  /**
   * Ensure pool size invariants.
   * The pool must either be below the soft-cap OR it must have no free values left
   */
  private synchronized void ensurePoolSizeInvariant() {
    Preconditions.checkState(!isMaxSizeSoftCapExceeded() || mFree.mNumBytes == 0);
  }

  /**
   * Initialize the list of buckets. Get the bucket sizes (and bucket lengths) from the bucket
   * sizes provider
   * @param inUseCounts map of current buckets and their in use counts
   */
  private synchronized void initBuckets(SparseIntArray inUseCounts) {
    Preconditions.checkNotNull(inUseCounts);

    // clear out all the buckets
    mBuckets.clear();

    // create the new buckets
    final SparseIntArray bucketSizes = mPoolParams.bucketSizes;
    if (bucketSizes != null) {
      for (int i = 0; i < bucketSizes.size(); ++i) {
        final int bucketSize = bucketSizes.keyAt(i);
        final int maxLength = bucketSizes.valueAt(i);
        int bucketInUseCount = inUseCounts.get(bucketSize, 0);
        mBuckets.put(
            bucketSize,
            new Bucket<V>(
                getSizeInBytes(bucketSize),
                maxLength,
                bucketInUseCount));
      }
      mAllowNewBuckets = false;
    } else {
      mAllowNewBuckets = true;
    }
  }

  /**
   * Gets rid of all free values in the pool
   * At the end of this method, mFreeSpace will be zero (reflecting that there are no more free
   * values in the pool). mUsedSpace will however not be reset, since that's a reflection of the
   * values that were allocated via the pool, but are in use elsewhere
   */
  @VisibleForTesting
  void trimToNothing() {
    final List<Bucket<V>> bucketsToTrim = new ArrayList<>(mBuckets.size());
    final SparseIntArray inUseCounts = new SparseIntArray();

    synchronized (this) {
      for (int i = 0; i < mBuckets.size(); ++i) {
        final Bucket<V> bucket = mBuckets.valueAt(i);
        if (bucket.getFreeListSize() > 0) {
          bucketsToTrim.add(bucket);
        }
        inUseCounts.put(mBuckets.keyAt(i), bucket.getInUseCount());
      }

      // reinitialize the buckets
      initBuckets(inUseCounts);

      // free up the stats
      mFree.reset();
      logStats();
    }

    // the pool parameters 'may' have changed.
    onParamsChanged();

    // Explicitly free all the values.
    // All the core data structures have now been reset. We no longer need to block other calls.
    // This is true even for a concurrent trim() call
    for (int i = 0; i < bucketsToTrim.size(); ++i) {
      final Bucket<V> bucket = bucketsToTrim.get(i);
      while (true) {
        // what happens if we run into an exception during the recycle. I'm going to ignore
        // these exceptions for now, and let the GC handle the rest of the to-be-recycled-bitmaps
        // in its usual fashion
        V item = bucket.pop();
        if (item == null) {
          break;
        }
        free(item);
      }
    }
  }


  /**
   * Trim the (free portion of the) pool so that the pool size is at or below the soft cap.
   * This will try to free up values in the free portion of the pool, until
   *   (a) the pool size is now below the soft cap configured OR
   *   (b) the free portion of the pool is empty
   */
  @VisibleForTesting
  synchronized void trimToSoftCap() {
    if (isMaxSizeSoftCapExceeded()) {
      trimToSize(mPoolParams.maxSizeSoftCap);
    }
  }

  /**
   * (Try to) trim the pool until its total space falls below the max size (soft cap). This will
   * get rid of values on the free list, until the free lists are empty, or we fall below the
   * max size; whichever comes first.
   * NOTE: It is NOT an error if we have eliminated all the free values, but the pool is still
   * above its max size (soft cap)
   * <p>
   * The approach we take is to go from the smallest sized bucket down to the largest sized
   * bucket. This may seem a bit counter-intuitive, but the rationale is that allocating
   * larger-sized values is more expensive than the smaller-sized ones, so we want to keep them
   * around for a while.
   * @param targetSize target size to trim to
   */
  @VisibleForTesting
  synchronized void trimToSize(int targetSize) {
    // find how much we need to free
    int bytesToFree = Math.min(mUsed.mNumBytes + mFree.mNumBytes - targetSize, mFree.mNumBytes);
    if (bytesToFree <= 0) {
      return;
    }
    if (FLog.isLoggable(FLog.VERBOSE)) {
      FLog.v(
          TAG,
          "trimToSize: TargetSize = %d; Initial Size = %d; Bytes to free = %d",
          targetSize,
          mUsed.mNumBytes + mFree.mNumBytes,
          bytesToFree);
    }
    logStats();

    // now walk through the buckets from the smallest to the largest. Keep freeing things
    // until we've gotten to what we want
    for (int i = 0; i < mBuckets.size(); ++i) {
      if (bytesToFree <= 0) {
        break;
      }
      Bucket<V> bucket = mBuckets.valueAt(i);
      while (bytesToFree > 0) {
        V value = bucket.pop();
        if (value == null) {
          break;
        }
        free(value);
        bytesToFree -= bucket.mItemSize;
        mFree.decrement(bucket.mItemSize);
      }
    }

    // dump stats at the end
    logStats();
    if (FLog.isLoggable(FLog.VERBOSE)) {
      FLog.v(
          TAG,
          "trimToSize: TargetSize = %d; Final Size = %d",
          targetSize,
          mUsed.mNumBytes + mFree.mNumBytes);
    }
  }


  /**
   * Gets the freelist for the specified bucket. Create the freelist if there isn't one
   * @param bucketedSize the bucket size
   * @return the freelist for the bucket
   */
  @VisibleForTesting
  synchronized Bucket<V> getBucket(int bucketedSize) {
    // get an existing bucket
    Bucket<V> bucket = mBuckets.get(bucketedSize);
    if (bucket != null || !mAllowNewBuckets) {
      return bucket;
    }

    // create a new bucket
    if (FLog.isLoggable(FLog.VERBOSE)) {
      FLog.v(TAG, "creating new bucket %s", bucketedSize);
    }
    Bucket<V> newBucket = newBucket(bucketedSize);
    mBuckets.put(bucketedSize, newBucket);
    return newBucket;
  }

  Bucket<V> newBucket(int bucketedSize) {
    return new Bucket<V>(
        /*itemSize*/getSizeInBytes(bucketedSize),
        /*maxLength*/Integer.MAX_VALUE,
        /*inUseLength*/0);
  }

  /**
   * Returns true if the pool size (sum of the used and the free portions) exceeds its 'max size'
   * soft cap as specified by the pool parameters.
   */
  @VisibleForTesting
  synchronized boolean isMaxSizeSoftCapExceeded() {
    final boolean isMaxSizeSoftCapExceeded =
        (mUsed.mNumBytes + mFree.mNumBytes) > mPoolParams.maxSizeSoftCap;
    if (isMaxSizeSoftCapExceeded) {
      mPoolStatsTracker.onSoftCapReached();
    }
    return isMaxSizeSoftCapExceeded;
  }

  /**
   * Can we allocate a value of size 'sizeInBytes' without exceeding the hard cap on the pool size?
   * If allocating this value will take the pool over the hard cap, we will first trim the pool down
   * to its soft cap, and then check again.
   * If the current used bytes + this new value will take us above the hard cap, then we return
   * false immediately - there is no point freeing up anything.
   * @param sizeInBytes the size (in bytes) of the value to allocate
   * @return true, if we can allocate this; false otherwise
   */
  @VisibleForTesting
  synchronized boolean canAllocate(int sizeInBytes) {
    int hardCap = mPoolParams.maxSizeHardCap;

    // even with our best effort we cannot ensure hard cap limit.
    // Return immediately - no point in trimming any space
    if ((mUsed.mNumBytes + sizeInBytes) > hardCap) {
      mPoolStatsTracker.onHardCapReached();
      return false;
    }

    // trim if we need to
    int softCap = mPoolParams.maxSizeSoftCap;
    if ((mUsed.mNumBytes + mFree.mNumBytes + sizeInBytes) > softCap) {
      trimToSize(softCap - sizeInBytes);
    }

    // check again to see if we're below the hard cap
    if (mUsed.mNumBytes + mFree.mNumBytes + sizeInBytes > hardCap) {
      mPoolStatsTracker.onHardCapReached();
      return false;
    }

    return true;
  }

  /**
   * Simple 'debug' logging of stats.
   * WARNING: The caller is responsible for synchronization
   */
  @SuppressLint("InvalidAccessToGuardedField")
  private void logStats() {
    if (FLog.isLoggable(FLog.VERBOSE)) {
      FLog.v(
          TAG,
          "Used = (%d, %d); Free = (%d, %d)",
          mUsed.mCount,
          mUsed.mNumBytes,
          mFree.mCount,
          mFree.mNumBytes);
    }
  }

  /**
   * Export memory stats regarding buckets used, memory caps, reused values.
   */
  public synchronized Map<String, Integer> getStats() {
    Map<String, Integer> stats = new HashMap<String, Integer>();
    for (int i = 0; i < mBuckets.size(); ++i) {
      final int bucketedSize = mBuckets.keyAt(i);
      final Bucket<V> bucket = mBuckets.valueAt(i);
      final String BUCKET_USED_KEY =
          PoolStatsTracker.BUCKETS_USED_PREFIX + getSizeInBytes(bucketedSize);
      stats.put(BUCKET_USED_KEY, bucket.getInUseCount());
    }

    stats.put(PoolStatsTracker.SOFT_CAP, mPoolParams.maxSizeSoftCap);
    stats.put(PoolStatsTracker.HARD_CAP, mPoolParams.maxSizeHardCap);
    stats.put(PoolStatsTracker.USED_COUNT, mUsed.mCount);
    stats.put(PoolStatsTracker.USED_BYTES, mUsed.mNumBytes);
    stats.put(PoolStatsTracker.FREE_COUNT, mFree.mCount);
    stats.put(PoolStatsTracker.FREE_BYTES, mFree.mNumBytes);

    return stats;
  }

  /**
   * A simple 'counter' that keeps track of the number of items (mCount) as well as the byte
   * mCount for the number of items
   * WARNING: this class is not synchronized - the caller must ensure the appropriate
   * synchronization
   */
  @NotThreadSafe
  @VisibleForTesting
  static class Counter {
    private static final String TAG = "com.facebook.imagepipeline.common.BasePool.Counter";

    int mCount;
    int mNumBytes;

    /**
     * Add a new item to the counter
     * @param numBytes size of the item in bytes
     */
    public void increment(int numBytes) {
      this.mCount++;
      this.mNumBytes += numBytes;
    }

    /**
     * 'Decrement' an item from the counter
     * @param numBytes size of the item in bytes
     */
    public void decrement(int numBytes) {
      if (this.mNumBytes >= numBytes && this.mCount > 0) {
        this.mCount--;
        this.mNumBytes -= numBytes;
      } else {
        FLog.wtf(
            TAG,
            "Unexpected decrement of %d. Current numBytes = %d, count = %d",
            numBytes,
            this.mNumBytes,
            this.mCount);
      }
    }

    /**
     * Reset the counter
     */
    public void reset() {
      this.mCount = 0;
      this.mNumBytes = 0;
    }
  }

  /**
   * An exception to indicate if the 'value' is invalid.
   */
  public static class InvalidValueException extends RuntimeException {
    public InvalidValueException(Object value) {
      super("Invalid value: " + value.toString());
    }
  }

  /**
   * An exception to indicate that the requested size was invalid
   */
  public static class InvalidSizeException extends RuntimeException {
    public InvalidSizeException(Object size) {
      super("Invalid size: " + size.toString());
    }
  }

  /**
   * A specific case of InvalidSizeException used to indicate that the requested size was too large
   */
  public static class SizeTooLargeException extends InvalidSizeException {
    public SizeTooLargeException(Object size) {
      super(size);
    }
  }

  /**
   * Indicates that the pool size will exceed the hard cap if we allocated a value
   * of size 'allocSize'
   */
  public static class PoolSizeViolationException extends RuntimeException {
    public PoolSizeViolationException(int hardCap, int usedBytes, int freeBytes, int allocSize) {
      super(
          "Pool hard cap violation?" +
              " Hard cap = " + hardCap +
              " Used size = " + usedBytes +
              " Free size = " + freeBytes +
              " Request size = " + allocSize);
    }
  }
}

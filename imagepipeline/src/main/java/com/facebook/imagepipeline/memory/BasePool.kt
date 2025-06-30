/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory

import android.annotation.SuppressLint
import android.util.SparseArray
import android.util.SparseIntArray
import androidx.annotation.VisibleForTesting
import com.facebook.common.internal.Sets
import com.facebook.common.internal.Throwables
import com.facebook.common.logging.FLog
import com.facebook.common.memory.MemoryTrimType
import com.facebook.common.memory.MemoryTrimmableRegistry
import com.facebook.common.memory.Pool
import com.facebook.imagepipeline.memory.BasePool.PoolSizeViolationException
import java.util.ArrayList
import java.util.HashMap
import javax.annotation.concurrent.GuardedBy
import javax.annotation.concurrent.NotThreadSafe
import kotlin.jvm.JvmField
import kotlin.math.min

/**
 * A base pool class that manages a pool of values (of type V).
 *
 * The pool is organized as a map. Each entry in the map is a free-list (modeled by a queue) of
 * entries for a given size. Some pools have a fixed set of buckets (aka bucketized sizes), while
 * others don't.
 *
 * The pool supports two main operations:
 * * [get(int)] - returns a value of size that's the same or larger than specified, hopefully from
 *   the pool; otherwise, this value is allocated (via the alloc function)
 * * [release(V)] - releases a value to the pool
 *
 * In addition, the pool subscribes to the [MemoryTrimmableRegistry], and responds to low-memory
 * events (calls to trim). Some percent (perhaps all) of the values in the pool are then released
 * (via the underlying free function), and no longer belong to the pool.
 *
 * Sizes There are 3 different notions of sizes we consider here (not all of them may be relevant
 * for each use case).
 * * Logical size is simply the size of the value in terms appropriate for the value. For example,
 *   for byte arrays, the size is simply the length. For a bitmap, the size is just the number of
 *   pixels.
 * * Bucketed size typically represents one of discrete set of logical sizes - such that each
 *   bucketed size can accommodate a range of logical sizes. For example, for byte arrays, using
 *   sizes that are powers of 2 for bucketed sizes allows these byte arrays to support a number of
 *   logical sizes.
 * * Finally, Size-in-bytes is exactly that - the size of the value in bytes.
 *
 * Logical Size and BucketedSize are both represented by the type parameter S, while size-in-bytes
 * is represented by an int.
 *
 * Each concrete subclass of the pool must implement the following methods
 * * [getBucketedSize(int)] - returns the bucketized size for the given request size
 * * [getBucketedSizeForValue(Object)] - returns the bucketized size for a given value
 * * [getSizeInBytes(int)] - gets the size in bytes for a given bucketized size
 * * [alloc(int)] - allocates a value of given size
 * * [free(Object)] - frees the value V Subclasses may optionally implement
 * * [onParamsChanged()] - called whenever this class determines to re-read the pool params
 * * [isReusable(Object)] - used to determine if a value can be reused or must be freed
 *
 * InUse values The pool keeps track of values currently in use (in addition to the free values in
 * the buckets). This is maintained in an IdentityHashSet (using reference equality for the values).
 * The in-use set helps with accounting/book-keeping; we also use this during [release(Object)] to
 * avoid messing with (freeing/reusing) values that are 'unknown' to the pool.
 *
 * PoolParams Pools are "configured" with a set of parameters (the PoolParams) supplied via a
 * provider. This set of parameters includes
 * * [PoolParams#maxSizeSoftCap] The size of a pool includes its used and free space. The maxSize
 *   setting for a pool is a soft cap on the overall size of the pool. A key point is that
 *   [get(int)] requests will not fail because the max size has been exceeded (unless the underlying
 *   [alloc(int)] function fails). However, the pool's free portion will be trimmed as much as
 *   possible so that the pool's size may fall below the max size. Note that when the free portion
 *   has fallen to zero, the pool may still be larger than its maxSizeSoftCap. On a
 *   [release(Object)] request, the value will be 'freed' instead of being added to the free portion
 *   of the pool, if the pool exceeds its maxSizeSoftCap. The invariant we want to maintain - see
 *   [ensurePoolSizeInvariant()] - is that the pool must be below the max size soft cap OR the free
 *   lists must be empty.
 * * [PoolParams#maxSizeHardCap] The hard cap is a stronger limit on the pool size. When this limit
 *   is reached, we first attempt to trim the pool. If the pool size is still over the hard, the
 *   [get(int)] call will fail with a [PoolSizeViolationException]
 * * [PoolParams#bucketSizes] The pool can be configured with a set of 'sizes' - a bucket is created
 *   for each such size. Additionally, each bucket can have a a max-length specified, which is the
 *   sum of the used and free items in that bucket. As with the MaxSize parameter above, the
 *   maxLength here is a soft cap, in that it will not cause an exception on get; it simply controls
 *   the release path. If the BucketSizes parameter is null, then the pool will dynamically create
 *   buckets on demand.
 */
abstract class BasePool<V : Any>(
    memoryTrimmableRegistry: MemoryTrimmableRegistry,
    poolParams: PoolParams,
    poolStatsTracker: PoolStatsTracker
) : Pool<V> {

  private val TAG: String = this.javaClass.simpleName.toString()

  /** The memory manager to register with */
  @JvmField
  val memoryTrimmableRegistry: MemoryTrimmableRegistry = checkNotNull(memoryTrimmableRegistry)

  /** Provider for pool parameters */
  @JvmField val poolParams: PoolParams = checkNotNull(poolParams)

  /** The buckets - representing different 'sizes' */

  // initialize the buckets
  @JvmField @VisibleForTesting val buckets: SparseArray<Bucket<V>> = SparseArray()

  /** An Identity hash-set to keep track of values by reference equality */
  @JvmField @VisibleForTesting val inUseValues: MutableSet<V?>

  /** Determines if new buckets can be created */
  private var allowNewBuckets = false

  /** tracks 'used space' - space allocated via the pool */
  @JvmField @VisibleForTesting @GuardedBy("this") val used: Counter

  /** tracks 'free space' in the pool */
  @JvmField @VisibleForTesting @GuardedBy("this") val free: Counter

  private val poolStatsTracker = checkNotNull(poolStatsTracker)

  private var ignoreHardCap = false

  constructor(
      memoryTrimmableRegistry: MemoryTrimmableRegistry,
      poolParams: PoolParams,
      poolStatsTracker: PoolStatsTracker,
      ignoreHardCap: Boolean
  ) : this(memoryTrimmableRegistry, poolParams, poolStatsTracker) {
    this.ignoreHardCap = ignoreHardCap
  }

  /** Finish pool initialization. */
  protected fun initialize() {
    memoryTrimmableRegistry.registerMemoryTrimmable(this)
    poolStatsTracker.setBasePool(this)
  }

  @Synchronized
  protected open fun getValue(bucket: Bucket<V>): V? =
      // noinspection deprecation
      bucket.get()

  /**
   * Gets a new 'value' from the pool, if available. Allocates a new value if necessary. If we need
   * to perform an allocation, - If the pool size exceeds the max-size soft cap, then we attempt to
   * trim the free portion of the pool. - If the pool size exceeds the max-size hard-cap (after
   * trimming), then we throw an [PoolSizeViolationException] Bucket length constraints are not
   * considered in this function
   *
   * @param size the logical size to allocate
   * @return a new value
   * @throws InvalidSizeException
   */
  override fun get(size: Int): V {
    ensurePoolSizeInvariant()

    var bucketedSize = this.getBucketedSize(size)
    var sizeInBytes = -1

    synchronized(this) {
      val bucket = this.getBucket(bucketedSize)
      if (bucket != null) {
        // find an existing value that we can reuse
        val value = this.getValue(bucket)
        if (value != null) {
          check(inUseValues.add(value))

          // It is possible that we got a 'larger' value than we asked for.
          // lets recompute size in bytes here
          bucketedSize = getBucketedSizeForValue(value)
          sizeInBytes = this.getSizeInBytes(bucketedSize)
          used.increment(sizeInBytes)
          free.decrement(sizeInBytes)
          poolStatsTracker.onValueReuse(sizeInBytes)
          logStats()
          if (FLog.isLoggable(FLog.VERBOSE)) {
            FLog.v(
                TAG,
                "get (reuse) (object, size) = (%x, %s)",
                System.identityHashCode(value),
                bucketedSize)
          }
          return value
        }
        // fall through
      }
      // check to see if we can allocate a value of the given size without exceeding the hard cap
      sizeInBytes = this.getSizeInBytes(bucketedSize)
      if (!canAllocate(sizeInBytes)) {
        throw PoolSizeViolationException(
            poolParams.maxSizeHardCap, used.numBytes, free.numBytes, sizeInBytes)
      }

      // Optimistically assume that allocation succeeds - if it fails, we need to undo those changes
      used.increment(sizeInBytes)
      bucket?.incrementInUseCount()
    }

    var value: V? = null
    try {
      // allocate the value outside the synchronized block, because it can be pretty expensive
      // we could have done the allocation inside the synchronized block,
      // but that would have blocked out other operations on the pool
      value = alloc(bucketedSize)
    } catch (e: Throwable) {
      // Assumption we made previously is not valid - allocation failed. We need to fix internal
      // counters.
      synchronized(this) {
        used.decrement(sizeInBytes)
        val bucket = this.getBucket(bucketedSize)
        bucket?.decrementInUseCount()
      }
      Throwables.propagateIfPossible(e)
    }

    // NOTE: We checked for hard caps earlier, and then did the alloc above. Now we need to
    // update state - but it is possible that a concurrent thread did a similar operation - with
    // the result being that we're now over the hard cap.
    // We are willing to live with that situation - especially since the trim call below should
    // be able to trim back memory usage.
    synchronized(this) {
      check(inUseValues.add(value))
      // If we're over the pool's max size, try to trim the pool appropriately
      trimToSoftCap()
      poolStatsTracker.onAlloc(sizeInBytes)
      logStats()
      if (FLog.isLoggable(FLog.VERBOSE)) {
        FLog.v(
            TAG,
            "get (alloc) (object, size) = (%x, %s)",
            System.identityHashCode(value),
            bucketedSize)
      }
    }

    return value!!
  }

  /**
   * Releases the given value to the pool. In a few cases, the value is 'freed' instead of being
   * released to the pool. If - the pool currently exceeds its max size OR - if the value does not
   * map to a bucket that's currently maintained by the pool, OR - if the bucket for the value
   * exceeds its maxLength, OR - if the value is not recognized by the pool then, the value is
   * 'freed'.
   *
   * @param value the value to release to the pool
   */
  override fun release(value: V) {
    checkNotNull(value)

    val bucketedSize = getBucketedSizeForValue(value)
    val sizeInBytes = this.getSizeInBytes(bucketedSize)
    synchronized(this) {
      val bucket = getBucketIfPresent(bucketedSize)
      if (!inUseValues.remove(value)) {
        // This value was not 'known' to the pool (i.e.) allocated via the pool.
        // Something is going wrong, so let's free the value and report soft error.
        FLog.e(
            TAG,
            "release (free, value unrecognized) (object, size) = (%x, %s)",
            System.identityHashCode(value),
            bucketedSize)
        free(value)
        poolStatsTracker.onFree(sizeInBytes)
      } else {
        // free the value, if
        // - pool exceeds maxSize
        // - there is no bucket for this value
        // - there is a bucket for this value, but it has exceeded its maxLength
        // - the value is not reusable
        // If no bucket was found for the value, simply free it
        // We should free the value if no bucket is found, or if the bucket length cap is exceeded.
        // However, if the pool max size softcap is exceeded, it may not always be best to free
        // *this* value.
        if (bucket == null ||
            bucket.isMaxLengthExceeded ||
            isMaxSizeSoftCapExceeded ||
            !isReusable(value)) {
          bucket?.decrementInUseCount()

          if (FLog.isLoggable(FLog.VERBOSE)) {
            FLog.v(
                TAG,
                "release (free) (object, size) = (%x, %s)",
                System.identityHashCode(value),
                bucketedSize)
          }
          free(value)
          used.decrement(sizeInBytes)
          poolStatsTracker.onFree(sizeInBytes)
        } else {
          bucket.release(value)
          free.increment(sizeInBytes)
          used.decrement(sizeInBytes)
          poolStatsTracker.onValueRelease(sizeInBytes)
          if (FLog.isLoggable(FLog.VERBOSE)) {
            FLog.v(
                TAG,
                "release (reuse) (object, size) = (%x, %s)",
                System.identityHashCode(value),
                bucketedSize)
          }
        }
      }
      logStats()
    }
  }

  /**
   * Trims the pool in response to low-memory states (invoked from MemoryManager) For now, we'll do
   * the simplest thing, and simply clear out the entire pool. We may consider more sophisticated
   * approaches later. In other words, we ignore the memoryTrimType parameter
   *
   * @param memoryTrimType the kind of trimming we want to perform
   */
  override fun trim(memoryTrimType: MemoryTrimType) {
    trimToNothing()
  }

  /**
   * Allocates a new 'value' with the given size
   *
   * @param bucketedSize the logical size to allocate
   * @return a new value
   */
  protected abstract fun alloc(bucketedSize: Int): V

  /**
   * Frees the 'value'
   *
   * @param value the value to free
   */
  @VisibleForTesting protected abstract fun free(value: V)

  /**
   * Gets the bucketed size (typically something the same or larger than the requested size)
   *
   * @param requestSize the logical request size
   * @return the 'bucketed' size
   * @throws InvalidSizeException, if the size of the value doesn't match the pool's constraints
   */
  protected abstract fun getBucketedSize(requestSize: Int): Int

  /**
   * Gets the bucketed size of the value
   *
   * @param value the value
   * @return bucketed size of the value
   * @throws InvalidSizeException, if the size of the value doesn't match the pool's constraints
   * @throws InvalidValueException, if the value is invalid
   */
  protected abstract fun getBucketedSizeForValue(value: V): Int

  /**
   * Gets the size in bytes for the given bucketed size
   *
   * @param bucketedSize the bucketed size
   * @return size in bytes
   */
  protected abstract fun getSizeInBytes(bucketedSize: Int): Int

  /**
   * The pool parameters may have changed. Subclasses can override this to update any state they
   * were maintaining
   */
  protected fun onParamsChanged() = Unit

  /**
   * Determines if the supplied value is 'reusable'. This is called during [release(Object)], and
   * determines if the value can be added to the freelists of the pool (for future reuse), or must
   * be released right away. Subclasses can override this to provide custom implementations
   *
   * @param value the value to test for reusability
   * @return true if the value is reusable
   */
  protected open fun isReusable(value: V): Boolean {
    checkNotNull(value)
    return true
  }

  /**
   * Ensure pool size invariants. The pool must either be below the soft-cap OR it must have no free
   * values left
   */
  @Synchronized
  private fun ensurePoolSizeInvariant() {
    if (ignoreHardCap) {
      return
    }
    check(!isMaxSizeSoftCapExceeded || free.numBytes == 0)
  }

  /**
   * Initialize the list of buckets. Get the bucket sizes (and bucket lengths) from the bucket sizes
   * provider
   *
   * @param inUseCounts map of current buckets and their in use counts
   */
  @Synchronized
  private fun legacyInitBuckets(inUseCounts: SparseIntArray) {
    checkNotNull(inUseCounts)

    // clear out all the buckets
    buckets.clear()

    // create the new buckets
    val bucketSizes = poolParams.bucketSizes
    if (bucketSizes != null) {
      for (i in 0 until bucketSizes.size()) {
        val bucketSize = bucketSizes.keyAt(i)
        val maxLength = bucketSizes.valueAt(i)
        val bucketInUseCount = inUseCounts[bucketSize, 0]
        buckets.put(
            bucketSize,
            Bucket(
                getSizeInBytes(bucketSize),
                maxLength,
                bucketInUseCount,
                poolParams.fixBucketsReinitialization))
      }
      allowNewBuckets = false
    } else {
      allowNewBuckets = true
    }
  }

  /**
   * Initialize the list of buckets. Get the bucket sizes (and bucket lengths) from the bucket sizes
   * provider
   */
  @Synchronized
  private fun initBuckets() {
    val bucketSizes = poolParams.bucketSizes

    // create the buckets
    if (bucketSizes != null) {
      fillBuckets(bucketSizes)
      allowNewBuckets = false
    } else {
      allowNewBuckets = true
    }
  }

  /**
   * Clears and fills `mBuckets` with buckets
   *
   * @param bucketSizes bucket size to bucket's max length
   */
  private fun fillBuckets(bucketSizes: SparseIntArray) {
    buckets.clear()
    for (i in 0 until bucketSizes.size()) {
      val bucketSize = bucketSizes.keyAt(i)
      val maxLength = bucketSizes.valueAt(i)
      buckets.put(
          bucketSize,
          Bucket(getSizeInBytes(bucketSize), maxLength, 0, poolParams.fixBucketsReinitialization))
    }
  }

  /** Clears and fills `mBuckets` with buckets */
  private fun refillBuckets(): MutableList<Bucket<V>> {
    val bucketsToTrim: MutableList<Bucket<V>> = ArrayList(buckets.size())

    var i = 0
    val len = buckets.size()
    while (i < len) {
      val oldBucket = checkNotNull(buckets.valueAt(i))
      val bucketSize = oldBucket.mItemSize
      val maxLength = oldBucket.mMaxLength
      val bucketInUseCount = oldBucket.inUseCount
      if (oldBucket.freeListSize > 0) {
        bucketsToTrim.add(oldBucket)
      }
      buckets.setValueAt(
          i,
          Bucket(
              getSizeInBytes(bucketSize),
              maxLength,
              bucketInUseCount,
              poolParams.fixBucketsReinitialization))
      ++i
    }

    return bucketsToTrim
  }

  /**
   * Gets rid of all free values in the pool At the end of this method, freeSpace will be zero
   * (reflecting that there are no more free values in the pool). usedSpace will however not be
   * reset, since that's a reflection of the values that were allocated via the pool, but are in use
   * elsewhere
   */
  @VisibleForTesting
  fun trimToNothing() {
    val bucketsToTrim: MutableList<Bucket<V>>

    synchronized(this) {
      if (poolParams.fixBucketsReinitialization) {
        bucketsToTrim = refillBuckets()
      } else {
        bucketsToTrim = ArrayList(buckets.size())
        val inUseCounts = SparseIntArray()

        for (i in 0 until buckets.size()) {
          val bucket = checkNotNull(buckets.valueAt(i))
          if (bucket.freeListSize > 0) {
            bucketsToTrim.add(bucket)
          }
          inUseCounts.put(buckets.keyAt(i), bucket.inUseCount)
        }

        legacyInitBuckets(inUseCounts)
      }
      // free up the stats
      free.reset()
      logStats()
    }

    // the pool parameters 'may' have changed.
    onParamsChanged()

    // Explicitly free all the values.
    // All the core data structures have now been reset. We no longer need to block other calls.
    // This is true even for a concurrent trim() call
    for (i in bucketsToTrim.indices) {
      val bucket = bucketsToTrim[i]
      while (true) {
        // what happens if we run into an exception during the recycle. I'm going to ignore
        // these exceptions for now, and let the GC handle the rest of the to-be-recycled-bitmaps
        // in its usual fashion
        val item = bucket.pop() ?: break
        free(item)
      }
    }
  }

  /**
   * Trim the (free portion of the) pool so that the pool size is at or below the soft cap. This
   * will try to free up values in the free portion of the pool, until (a) the pool size is now
   * below the soft cap configured OR (b) the free portion of the pool is empty
   */
  @VisibleForTesting
  @Synchronized
  fun trimToSoftCap() {
    if (isMaxSizeSoftCapExceeded) {
      trimToSize(poolParams.maxSizeSoftCap)
    }
  }

  /**
   * (Try to) trim the pool until its total space falls below the max size (soft cap). This will get
   * rid of values on the free list, until the free lists are empty, or we fall below the max size;
   * whichever comes first. NOTE: It is NOT an error if we have eliminated all the free values, but
   * the pool is still above its max size (soft cap)
   *
   * The approach we take is to go from the smallest sized bucket down to the largest sized bucket.
   * This may seem a bit counter-intuitive, but the rationale is that allocating larger-sized values
   * is more expensive than the smaller-sized ones, so we want to keep them around for a while.
   *
   * @param targetSize target size to trim to
   */
  @VisibleForTesting
  @Synchronized
  fun trimToSize(targetSize: Int) {
    // find how much we need to free
    var bytesToFree =
        min((used.numBytes + free.numBytes - targetSize).toDouble(), free.numBytes.toDouble())
            .toInt()
    if (bytesToFree <= 0) {
      return
    }
    if (FLog.isLoggable(FLog.VERBOSE)) {
      FLog.v(
          TAG,
          "trimToSize: TargetSize = %d; Initial Size = %d; Bytes to free = %d",
          targetSize,
          used.numBytes + free.numBytes,
          bytesToFree)
    }
    logStats()

    // now walk through the buckets from the smallest to the largest. Keep freeing things
    // until we've gotten to what we want
    for (i in 0 until buckets.size()) {
      if (bytesToFree <= 0) {
        break
      }
      val bucket = checkNotNull(buckets.valueAt(i))
      while (bytesToFree > 0) {
        val value = bucket.pop() ?: break
        free(value)
        bytesToFree -= bucket.mItemSize
        free.decrement(bucket.mItemSize)
      }
    }

    // dump stats at the end
    logStats()
    if (FLog.isLoggable(FLog.VERBOSE)) {
      FLog.v(
          TAG,
          "trimToSize: TargetSize = %d; Final Size = %d",
          targetSize,
          used.numBytes + free.numBytes)
    }
  }

  /**
   * Gets the freelist for the specified bucket if it exists.
   *
   * @param bucketedSize the bucket size
   * @return the freelist for the bucket
   */
  @Synchronized
  private fun getBucketIfPresent(bucketedSize: Int): Bucket<V>? = buckets[bucketedSize]

  /**
   * Gets the freelist for the specified bucket. Create the freelist if there isn't one
   *
   * @param bucketedSize the bucket size
   * @return the freelist for the bucket
   */
  @VisibleForTesting
  @Synchronized
  fun getBucket(bucketedSize: Int): Bucket<V>? {
    // get an existing bucket
    val bucket = buckets[bucketedSize]
    if (bucket != null || !allowNewBuckets) {
      return bucket
    }

    // create a new bucket
    if (FLog.isLoggable(FLog.VERBOSE)) {
      FLog.v(TAG, "creating new bucket %s", bucketedSize)
    }
    val newBucket = newBucket(bucketedSize)
    buckets.put(bucketedSize, newBucket)
    return newBucket
  }

  open fun newBucket(bucketedSize: Int): Bucket<V> =
      Bucket(
          /*itemSize*/
          getSizeInBytes(bucketedSize), /*maxLength*/
          Int.MAX_VALUE, /*inUseLength*/
          0,
          poolParams.fixBucketsReinitialization)

  @get:Synchronized
  @get:VisibleForTesting
  val isMaxSizeSoftCapExceeded: Boolean
    /**
     * Returns true if the pool size (sum of the used and the free portions) exceeds its 'max size'
     * soft cap as specified by the pool parameters.
     */
    get() {
      val isMaxSizeSoftCapExceeded = (used.numBytes + free.numBytes) > poolParams.maxSizeSoftCap
      if (isMaxSizeSoftCapExceeded) {
        poolStatsTracker.onSoftCapReached()
      }
      return isMaxSizeSoftCapExceeded
    }

  /**
   * Can we allocate a value of size 'sizeInBytes' without exceeding the hard cap on the pool size?
   * If allocating this value will take the pool over the hard cap, we will first trim the pool down
   * to its soft cap, and then check again. If the current used bytes + this new value will take us
   * above the hard cap, then we return false immediately - there is no point freeing up anything.
   *
   * @param sizeInBytes the size (in bytes) of the value to allocate
   * @return true, if we can allocate this; false otherwise
   */
  @VisibleForTesting
  @Synchronized
  fun canAllocate(sizeInBytes: Int): Boolean {
    if (ignoreHardCap) {
      return true
    }

    val hardCap = poolParams.maxSizeHardCap

    // even with our best effort we cannot ensure hard cap limit.
    // Return immediately - no point in trimming any space
    if (sizeInBytes > hardCap - used.numBytes) {
      poolStatsTracker.onHardCapReached()
      return false
    }

    // trim if we need to
    val softCap = poolParams.maxSizeSoftCap
    if (sizeInBytes > softCap - (used.numBytes + free.numBytes)) {
      trimToSize(softCap - sizeInBytes)
    }

    // check again to see if we're below the hard cap
    if (sizeInBytes > hardCap - (used.numBytes + free.numBytes)) {
      poolStatsTracker.onHardCapReached()
      return false
    }

    return true
  }

  /** Simple 'debug' logging of stats. WARNING: The caller is responsible for synchronization */
  @SuppressLint("InvalidAccessToGuardedField")
  private fun logStats() {
    if (FLog.isLoggable(FLog.VERBOSE)) {
      FLog.v(
          TAG,
          "Used = (%d, %d); Free = (%d, %d)",
          used.count,
          used.numBytes,
          free.count,
          free.numBytes)
    }
  }

  @get:Synchronized
  val stats: Map<String, Int>
    /** Export memory stats regarding buckets used, memory caps, reused values. */
    get() {
      val stats: MutableMap<String, Int> = HashMap()
      for (i in 0 until buckets.size()) {
        val bucketedSize = buckets.keyAt(i)
        val bucket = checkNotNull(buckets.valueAt(i))
        val BUCKET_USED_KEY = PoolStatsTracker.BUCKETS_USED_PREFIX + getSizeInBytes(bucketedSize)
        stats[BUCKET_USED_KEY] = bucket.inUseCount
      }

      stats[PoolStatsTracker.SOFT_CAP] = poolParams.maxSizeSoftCap
      stats[PoolStatsTracker.HARD_CAP] = poolParams.maxSizeHardCap
      stats[PoolStatsTracker.USED_COUNT] = used.count
      stats[PoolStatsTracker.USED_BYTES] = used.numBytes
      stats[PoolStatsTracker.FREE_COUNT] = free.count
      stats[PoolStatsTracker.FREE_BYTES] = free.numBytes

      return stats
    }

  /**
   * Creates a new instance of the pool.
   *
   * @param poolParams pool parameters
   * @param poolStatsTracker
   */
  init {
    if (poolParams.fixBucketsReinitialization) {
      initBuckets()
    } else {
      legacyInitBuckets(SparseIntArray(0))
    }

    inUseValues = Sets.newIdentityHashSet()

    free = Counter()
    used = Counter()
  }

  /**
   * A simple 'counter' that keeps track of the number of items (mCount) as well as the byte mCount
   * for the number of items WARNING: this class is not synchronized - the caller must ensure the
   * appropriate synchronization
   */
  @NotThreadSafe
  @VisibleForTesting
  class Counter {
    @JvmField var count: Int = 0
    @JvmField var numBytes: Int = 0

    /**
     * Add a new item to the counter
     *
     * @param numBytes size of the item in bytes
     */
    fun increment(numBytes: Int) {
      count++
      this.numBytes += numBytes
    }

    /**
     * 'Decrement' an item from the counter
     *
     * @param numBytes size of the item in bytes
     */
    fun decrement(numBytes: Int) {
      if (this.numBytes >= numBytes && this.count > 0) {
        count--
        this.numBytes -= numBytes
      } else {
        FLog.wtf(
            TAG,
            "Unexpected decrement of %d. Current numBytes = %d, count = %d",
            numBytes,
            this.numBytes,
            this.count)
      }
    }

    /** Reset the counter */
    fun reset() {
      this.count = 0
      this.numBytes = 0
    }

    companion object {
      private const val TAG = "com.facebook.imagepipeline.memory.BasePool.Counter"
    }
  }

  /** An exception to indicate if the 'value' is invalid. */
  class InvalidValueException(value: Any) : RuntimeException("Invalid value: $value")

  /** An exception to indicate that the requested size was invalid */
  open class InvalidSizeException(size: Any) : RuntimeException("Invalid size: $size")

  /**
   * A specific case of InvalidSizeException used to indicate that the requested size was too large
   */
  class SizeTooLargeException(size: Any) : InvalidSizeException(size)

  /**
   * Indicates that the pool size will exceed the hard cap if we allocated a value of size
   * 'allocSize'
   */
  class PoolSizeViolationException(hardCap: Int, usedBytes: Int, freeBytes: Int, allocSize: Int) :
      RuntimeException(
          "Pool hard cap violation? Hard cap = ${hardCap} Used size = ${usedBytes} Free size = ${freeBytes} Request size = ${allocSize}")
}

/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory;

import android.util.SparseIntArray;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

/**
 * Config parameters for pools ({@link BasePool}. Supplied via a provider.
 *
 * <p>{@link #bucketSizes} The pool can be configured with a set of 'sizes' - a bucket is created
 * for each such size. Additionally, each bucket can have a a max-length specified, which is the sum
 * of the used and free items in that bucket. As with the MaxSize parameter above, the maxLength
 * here is a soft cap, in that it will not cause an exception on get; it simply controls the release
 * path When the bucket sizes are specified upfront, the pool may still get requests for non
 * standard sizes. Such cases are treated as plain alloc/free calls i.e. the values are not
 * maintained in the pool. If this parameter is null, then the pool will create buckets on demand
 *
 * <p>{@link #minBucketSize} This represents the minimum size of the buckets in the pool. This
 * assures that all buckets can hold any element larger or equal to this size.
 *
 * <p>{@link #maxBucketSize} This represents the maximum size of the buckets in the pool. This
 * restricts all buckets to only accept elements smaller or equal to this size. If this size is
 * exceeded, an exception will be thrown.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class PoolParams {
  /** If maxNumThreads is set to this level, the pool doesn't actually care what it is */
  public static final int IGNORE_THREADS = -1;

  public final @Nullable SparseIntArray bucketSizes;
  public final int minBucketSize;
  public final int maxBucketSize;

  public boolean fixBucketsReinitialization;

  /**
   * The maximum number of threads that may be accessing this pool.
   *
   * <p>Pool implementations may or may not need this to be set.
   */
  public final int maxNumThreads;

  /**
   * Set up pool params
   *
   * @param bucketSizes (optional) bucket sizes and lengths for the pool
   */
  public PoolParams(@Nullable SparseIntArray bucketSizes) {
    this(bucketSizes, 0, Integer.MAX_VALUE, IGNORE_THREADS);
  }

  /**
   * Set up pool params
   *
   * @param bucketSizes (optional) bucket sizes and lengths for the pool
   * @param minBucketSize min bucket size for the pool
   * @param maxBucketSize max bucket size for the pool
   * @param maxNumThreads the maximum number of threads in the pool, or -1 if the pool doesn't care
   */
  public PoolParams(
      @Nullable SparseIntArray bucketSizes,
      int minBucketSize,
      int maxBucketSize,
      int maxNumThreads) {
    this.bucketSizes = bucketSizes;
    this.minBucketSize = minBucketSize;
    this.maxBucketSize = maxBucketSize;
    this.maxNumThreads = maxNumThreads;
  }
}

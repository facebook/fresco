/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers

/**
 * How much new data, and how much time, a partial result has to carry to be worth propagating — for
 * a response whose decoder cannot make progress on one that carries too little and spends the work
 * for nothing.
 *
 * The first partial result of a response body waits for [initialBytes]; every one after it waits
 * for a further [incrementBytes] measured from the size the body had reached when the previous one
 * actually went out, so a read that overshoots a threshold carries the following ones up with it
 * rather than releasing them all at once. Zero drops the corresponding requirement.
 */
data class PartialResultThrottle(
    val initialBytes: Int,
    val incrementBytes: Int,
    /**
     * Replaces the interval the pipeline would otherwise space partial results by. Zero leaves that
     * interval in place rather than removing it, so a throttle that only sets byte thresholds keeps
     * the cadence the response would have had.
     */
    val minIntervalMs: Long,
) {

  /**
   * @param sizeBytes bytes of this response body accumulated so far
   * @param lastPropagatedSizeBytes what [sizeBytes] was when this body last propagated a partial
   *   result, or 0 if it has not propagated one
   */
  fun allowsPropagationAt(sizeBytes: Int, lastPropagatedSizeBytes: Int): Boolean =
      // A size below the one already propagated means a new response body on a fetch state that
      // kept the old one's count: a producer can read a second body after a redirect or a retry
      // without going back through NetworkFetchProducer.onResponse, where the count is reset.
      // Treated as the start of a body rather than as a negative increment, which would suppress
      // every partial result until the new body outgrew the old one.
      if (lastPropagatedSizeBytes == 0 || sizeBytes < lastPropagatedSizeBytes) {
        sizeBytes >= initialBytes
      } else {
        // Subtraction rather than lastPropagatedSizeBytes + incrementBytes: both sides are
        // non-negative, so a nonsensically large configured increment cannot overflow into
        // "propagate everything".
        sizeBytes - lastPropagatedSizeBytes >= incrementBytes
      }

  fun minIntervalMsOr(fallbackMs: Long): Long = if (minIntervalMs > 0) minIntervalMs else fallbackMs

  companion object {
    /** Propagates every partial result, i.e. what a fetcher that sets no throttle gets. */
    @JvmField val NONE: PartialResultThrottle = PartialResultThrottle(0, 0, 0)
  }
}

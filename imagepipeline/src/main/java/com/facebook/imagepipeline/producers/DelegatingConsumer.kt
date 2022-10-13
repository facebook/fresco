/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers

/** Delegating consumer. */
abstract class DelegatingConsumer<I, O>(val consumer: Consumer<O>) : BaseConsumer<I>() {

  override fun onFailureImpl(t: Throwable) {
    consumer.onFailure(t)
  }

  override fun onCancellationImpl() {
    consumer.onCancellation()
  }

  override fun onProgressUpdateImpl(progress: Float) {
    consumer.onProgressUpdate(progress)
  }
}

/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers

/** Callbacks that are called when something changes in a request sequence. */
interface ProducerContextCallbacks {

  /** Method that is called when a client cancels the request. */
  fun onCancellationRequested()

  /** Method that is called when a request is no longer a prefetch, or vice versa. */
  fun onIsPrefetchChanged()

  /** Method that is called when intermediate results start or stop being expected. */
  fun onIsIntermediateResultExpectedChanged()

  /** Method that is called when the priority of the request changes. */
  fun onPriorityChanged()
}

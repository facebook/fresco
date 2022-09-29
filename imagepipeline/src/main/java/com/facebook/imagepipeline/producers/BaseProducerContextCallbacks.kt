/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers

/** Empty implementation of [ProducerContextCallbacks]. */
open class BaseProducerContextCallbacks : ProducerContextCallbacks {
  override fun onCancellationRequested() = Unit

  override fun onIsPrefetchChanged() = Unit

  override fun onIsIntermediateResultExpectedChanged() = Unit

  override fun onPriorityChanged() = Unit
}

/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.debug

import com.facebook.common.references.SharedReference

/**
 * Tracker for leaks that occur when a [com.facebook.common.references.CloseableReference] is not
 * closed.
 */
interface CloseableReferenceLeakTracker {

  interface Listener {
    fun onCloseableReferenceLeak(reference: SharedReference<Any?>, stacktrace: Throwable?)
  }

  fun trackCloseableReferenceLeak(reference: SharedReference<Any?>, stacktrace: Throwable?)

  fun setListener(listener: Listener?)

  /** Indicate whether or not a listener is set. */
  val isSet: Boolean
}

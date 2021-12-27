/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.debug

import com.facebook.common.references.SharedReference

class NoOpCloseableReferenceLeakTracker : CloseableReferenceLeakTracker {

  override fun trackCloseableReferenceLeak(
      reference: SharedReference<Any?>,
      stacktrace: Throwable?
  ) = Unit

  override fun setListener(listener: CloseableReferenceLeakTracker.Listener?) = Unit

  override val isSet: Boolean
    get() = false
}

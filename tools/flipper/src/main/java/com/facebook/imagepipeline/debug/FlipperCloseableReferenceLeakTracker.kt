/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.debug

import android.util.Log
import com.facebook.common.references.SharedReference

class FlipperCloseableReferenceLeakTracker : CloseableReferenceLeakTracker {

  private var listener: CloseableReferenceLeakTracker.Listener? = null

  override fun trackCloseableReferenceLeak(
      reference: SharedReference<Any?>,
      stacktrace: Throwable?
  ) {
    if (listener == null) {
      Log.w("FRESCO", "No Flipper listener registered to track CloseableReference leak.")
      return
    }
    listener?.onCloseableReferenceLeak(reference, stacktrace)
  }

  override fun setListener(listener: CloseableReferenceLeakTracker.Listener?) {
    this.listener = listener
  }

  override val isSet: Boolean
    get() = (listener != null)
}

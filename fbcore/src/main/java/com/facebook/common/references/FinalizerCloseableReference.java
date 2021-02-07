/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.references;

import com.facebook.common.logging.FLog;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.STRICT)
public class FinalizerCloseableReference<T> extends CloseableReference<T> {

  private static final String TAG = "FinalizerCloseableReference";

  /*package*/ FinalizerCloseableReference(
      T t,
      ResourceReleaser<T> resourceReleaser,
      LeakHandler leakHandler,
      @Nullable Throwable stacktrace) {
    super(t, resourceReleaser, leakHandler, stacktrace);
  }

  @Override
  public CloseableReference<T> clone() {
    // No ref counting
    return this;
  }

  @Override
  public void close() {
    // Nop
  }

  @Override
  protected void finalize() throws Throwable {
    try {
      // We put synchronized here so that lint doesn't warn about accessing mIsClosed, which is
      // guarded by this. Lint isn't aware of finalize semantics.
      synchronized (this) {
        if (mIsClosed) {
          return;
        }
      }

      T ref = mSharedReference.get();
      FLog.w(
          TAG,
          "Finalized without closing: %x %x (type = %s)",
          System.identityHashCode(this),
          System.identityHashCode(mSharedReference),
          ref == null ? null : ref.getClass().getName());

      mSharedReference.deleteReference();
    } finally {
      super.finalize();
    }
  }
}

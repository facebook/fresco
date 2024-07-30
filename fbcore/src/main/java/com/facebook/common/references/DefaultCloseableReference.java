/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.references;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.logging.FLog;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class DefaultCloseableReference<T> extends CloseableReference<T> {

  private static final String TAG = "DefaultCloseableReference";

  private DefaultCloseableReference(
      SharedReference<T> sharedReference,
      @Nullable LeakHandler leakHandler,
      @Nullable Throwable stacktrace) {
    super(sharedReference, leakHandler, stacktrace);
  }

  /*package*/ DefaultCloseableReference(
      T t,
      ResourceReleaser<T> resourceReleaser,
      LeakHandler leakHandler,
      @Nullable Throwable stacktrace) {
    super(t, resourceReleaser, leakHandler, stacktrace, true);
  }

  @Override
  public CloseableReference<T> clone() {
    Preconditions.checkState(isValid());
    return new DefaultCloseableReference<T>(
        mSharedReference, mLeakHandler, mStacktrace != null ? new Throwable() : null);
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

      if (mLeakHandler != null) {
        mLeakHandler.reportLeak((SharedReference<Object>) mSharedReference, mStacktrace);
      }

      close();
    } finally {
      super.finalize();
    }
  }
}

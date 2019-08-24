/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.common.references;

import com.facebook.common.internal.Preconditions;
import javax.annotation.Nullable;

public class RefCountCloseableReference<T> extends CloseableReference<T> {

  private RefCountCloseableReference(
      SharedReference<T> sharedReference, LeakHandler leakHandler, @Nullable Throwable stacktrace) {
    super(sharedReference, leakHandler, stacktrace);
  }

  /*package*/ RefCountCloseableReference(
      T t,
      ResourceReleaser<T> resourceReleaser,
      LeakHandler leakHandler,
      @Nullable Throwable stacktrace) {
    super(t, resourceReleaser, leakHandler, stacktrace);
  }

  @Override
  public CloseableReference<T> clone() {
    Preconditions.checkState(isValid());
    return new RefCountCloseableReference<T>(mSharedReference, mLeakHandler, mStacktrace);
  }
}

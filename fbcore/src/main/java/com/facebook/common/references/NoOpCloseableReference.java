/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.references;

import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.STRICT)
public class NoOpCloseableReference<T> extends CloseableReference<T> {

  /*package*/ NoOpCloseableReference(
      T t,
      ResourceReleaser<T> resourceReleaser,
      LeakHandler leakHandler,
      @Nullable Throwable stacktrace) {
    super(t, resourceReleaser, leakHandler, stacktrace, false);
  }

  @Override
  public CloseableReference<T> clone() {
    // No ref counting
    return this;
  }

  @Override
  public CloseableReference<T> cloneOrNull() {
    // No ref counting
    return this;
  }

  @Override
  public void close() {
    // Nop
  }

  @Override
  public boolean isValid() {
    // Always valid
    return true;
  }
}

/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.common.references;

import javax.annotation.Nullable;

public class NoOpCloseableReference<T> extends CloseableReference<T> {

  /*package*/ NoOpCloseableReference(
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
}

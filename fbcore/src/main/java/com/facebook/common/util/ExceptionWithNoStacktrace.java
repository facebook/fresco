/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.util;

import com.facebook.infer.annotation.Nullsafe;

/**
 * Instantiate an exception with an empty stacktrace. This is more performant than instantiating a
 * regular exception since it doesn't incur the cost of getting the stack trace.
 */
@Nullsafe(Nullsafe.Mode.STRICT)
public class ExceptionWithNoStacktrace extends Exception {
  @Override
  public synchronized Throwable fillInStackTrace() {
    return this;
  }

  public ExceptionWithNoStacktrace(String detailMessage) {
    super(detailMessage);
  }
}

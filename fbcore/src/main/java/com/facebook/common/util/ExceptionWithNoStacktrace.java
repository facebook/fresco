/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.common.util;

/**
 * Instantiate an exception with an empty stacktrace. This is more performant than instantiating
 * a regular exception since it doesn't incur the cost of getting the stack trace.
 */
public class ExceptionWithNoStacktrace extends Exception {
  @Override
  public synchronized Throwable fillInStackTrace() {
    return this;
  }
  public ExceptionWithNoStacktrace(String detailMessage) {
    super(detailMessage);
  }
}

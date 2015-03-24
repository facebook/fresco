/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.decoder;

public class DecodeException extends RuntimeException {
  public DecodeException(String message) {
    super(message);
  }

  public DecodeException(String message, Throwable t) {
    super(message, t);
  }
}

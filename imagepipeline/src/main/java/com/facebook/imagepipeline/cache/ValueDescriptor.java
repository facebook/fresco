/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.imagepipeline.cache;

/**
 * Interface used to get the information about the values.
 */
public interface ValueDescriptor<V> {

  /** Returns the size in bytes of the given value. */
  int getSizeInBytes(V value);
}

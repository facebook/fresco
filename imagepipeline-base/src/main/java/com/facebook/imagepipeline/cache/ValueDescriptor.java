/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.cache;

/**
 * Interface used to get the information about the values.
 */
public interface ValueDescriptor<V> {

  /** Returns the size in bytes of the given value. */
  int getSizeInBytes(V value);
}

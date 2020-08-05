// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package com.facebook.cache.common;

import javax.annotation.Nullable;

public interface HasDebugData {

  /** Return a string describing this object and to be used for debugging or logging */
  @Nullable
  String getDebugData();
}

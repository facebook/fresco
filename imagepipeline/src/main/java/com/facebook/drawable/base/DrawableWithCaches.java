/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawable.base;

/**
 * A drawable can implement this interface to be notified when it might be convenient to
 * drop its caches in order conserve memory. This is best effort and the Drawable should not
 * depend on it being called.
 */
public interface DrawableWithCaches {

  /**
   * Informs the Drawable to drop its caches.
   */
  void dropCaches();
}

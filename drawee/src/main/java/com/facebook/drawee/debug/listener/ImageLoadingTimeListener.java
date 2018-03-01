/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.drawee.debug.listener;

/** Implement this interface to notify UI that the final Image has been set. */
public interface ImageLoadingTimeListener {
  void onFinalImageSet(long finalImageTimeMs);
}

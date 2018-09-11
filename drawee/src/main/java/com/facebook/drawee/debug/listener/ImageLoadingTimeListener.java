/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.drawee.debug.listener;

/** Implement this interface to notify UI that the final Image has been set. */
public interface ImageLoadingTimeListener {
  void onFinalImageSet(long finalImageTimeMs);
}

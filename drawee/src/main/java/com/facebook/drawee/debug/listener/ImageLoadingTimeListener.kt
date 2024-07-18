/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.debug.listener

/** Implement this fun interface to notify UI that the final Image has been set. */
interface ImageLoadingTimeListener {
  fun onFinalImageSet(finalImageTimeMs: Long)
}

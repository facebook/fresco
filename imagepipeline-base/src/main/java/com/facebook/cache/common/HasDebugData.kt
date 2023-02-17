/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.cache.common

interface HasDebugData {
  /** Return a string describing this object and to be used for debugging or logging */
  val debugData: String?
}

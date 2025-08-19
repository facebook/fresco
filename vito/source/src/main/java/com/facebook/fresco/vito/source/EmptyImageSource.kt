/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.source

/**
 * An empty image source. This is used to indicate that no image will be displayed. A reason must be
 * supplied to indicate why the image is empty.
 */
open class EmptyImageSource(val reason: String) : ImageSource {

  override fun equals(other: Any?): Boolean {
    // We ignore the reason to avoid unnecessary image reloads
    return this === other || other is EmptyImageSource
  }

  // We ignore the reason to avoid unnecessary image reloads
  override fun hashCode(): Int = 0

  override fun getClassNameString(): String = "EmptyImageSource($reason)"
}

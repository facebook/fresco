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
 *
 * @param reason free-text description of why no image is available. Surfaces via
 *   [getClassNameString] in debug tooling and bug-report attachments.
 * @param isExpectedEmpty when `true`, the caller is signalling that having no image is a legitimate
 *   outcome (e.g. a page that genuinely has no profile picture) rather than a failure. The Fresco
 *   Vito controllers can opt into routing such requests through the `onEmptyEvent` listener
 *   callback instead of firing `onFailure`. Default `false` preserves today's behavior. Controllers
 *   only honor this flag when their own config flag is on (see `FrescoVitoConfig`).
 */
open class EmptyImageSource
@JvmOverloads
constructor(val reason: String, val isExpectedEmpty: Boolean = false) : ImageSource {

  override fun equals(other: Any?): Boolean {
    // We ignore the reason and isExpectedEmpty to avoid unnecessary image reloads.
    return this === other || other is EmptyImageSource
  }

  // We ignore the reason and isExpectedEmpty to avoid unnecessary image reloads.
  override fun hashCode(): Int = 0

  override fun getClassNameString(): String =
      "EmptyImageSource($reason${if (isExpectedEmpty) ", expected" else ""})"
}

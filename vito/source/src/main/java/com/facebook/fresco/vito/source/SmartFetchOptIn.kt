/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.source

enum class SmartFetchOptIn {
  /** Smart Fetch on, no disk cache fallback. */
  ENABLED_NO_FALLBACK,

  /** Smart Fetch on, with disk cache fallback. */
  ENABLED_WITH_FALLBACK,
}

/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.urimod

sealed interface FetchStrategy

data object NoPrefetchInOnPrepareStrategy : FetchStrategy

enum class SmartFetchStrategy : FetchStrategy {
  DEFAULT,
  MAIN_THREAD,
  DISK_CACHE_TIMEOUT,
  ;

  override fun toString(): String = "SmartFetchStrategy: ${this.name}"
}

enum class ClassicFetchStrategy : FetchStrategy {
  DEFAULT,
  APP_DISABLED,
  PRODUCT_DISABLED,
  APP_STARTING,
  MAIN_THREAD,
  DISK_CACHE_TIMEOUT,
  DISK_CACHE_HIT,
  ;

  override fun toString(): String = "ClassicFetchStrategy: ${this.name}"
}

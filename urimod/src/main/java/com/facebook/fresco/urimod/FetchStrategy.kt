// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

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
  APP_DISABLED,
  PRODUCT_DISABLED,
  APP_STARTING,
  MAIN_THREAD,
  DISK_CACHE_TIMEOUT,
  DISK_CACHE_HIT,
  ;

  override fun toString(): String = "ClassicFetchStrategy: ${this.name}"
}

/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers

/**
 * Base class for [NetworkFetcher].
 *
 * Intermediate results are propagated.
 *
 * {#code getExtraMap} returns null.
 */
abstract class BaseNetworkFetcher<FETCH_STATE : FetchState?> : NetworkFetcher<FETCH_STATE> {

  override fun shouldPropagate(fetchState: FETCH_STATE): Boolean = true

  override fun onFetchCompletion(fetchState: FETCH_STATE, byteSize: Int) {
    // no-op
  }

  override fun getExtraMap(fetchState: FETCH_STATE, byteSize: Int): Map<String, String>? = null
}

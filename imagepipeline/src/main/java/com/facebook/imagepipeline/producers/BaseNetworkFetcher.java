/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import javax.annotation.Nullable;

import java.util.Map;

/**
 * Base class for {@link NetworkFetcher}.
 *
 * <p> Intermediate results are propagated.
 * <p> {#code getExtraMap} returns null.
 */
public abstract class BaseNetworkFetcher<FETCH_STATE extends FetchState>
    implements NetworkFetcher<FETCH_STATE> {

  @Override
  public boolean shouldPropagate(FETCH_STATE fetchState) {
    return true;
  }

  @Override
  public void onFetchCompletion(FETCH_STATE fetchState, int byteSize) {
    // no-op
  }

  @Nullable
  @Override
  public Map<String, String> getExtraMap(FETCH_STATE fetchState, int byteSize) {
    return null;
  }
}

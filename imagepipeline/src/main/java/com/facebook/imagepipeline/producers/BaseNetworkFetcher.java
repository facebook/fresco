/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import java.util.Map;
import javax.annotation.Nullable;

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

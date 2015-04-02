/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import android.net.Uri;

import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.memory.PooledByteBuffer;

/**
 * Network fetcher that uses the simplest Android stack.
 *
 * <p> Apps requiring more sophisticated networking should implement their own
 * {@link NetworkFetcher}.
 */
public class HttpUrlConnectionNetworkFetcher extends BaseNetworkFetcher<FetchState> {

  private static final int NUM_NETWORK_THREADS = 3;

  private final Executor mExecutor;

  public HttpUrlConnectionNetworkFetcher() {
    mExecutor = Executors.newFixedThreadPool(NUM_NETWORK_THREADS);
  }

  @Override
  public FetchState createFetchState(
      Consumer<CloseableReference<PooledByteBuffer>> consumer,
      ProducerContext context) {
    return new FetchState(consumer, context);
  }

  @Override
  public void fetch(final FetchState fetchState, final Callback callback) {
    mExecutor.execute(
        new Runnable() {
          @Override
          public void run() {
            HttpURLConnection connection = null;
            try {
              Uri uri = fetchState.getUri();
              URL url = new URL(uri.toString());
              connection = (HttpURLConnection) url.openConnection();
              InputStream is = connection.getInputStream();
              callback.onResponse(is, -1);
            } catch (Exception e) {
              callback.onFailure(e);
            } finally {
              if (connection != null) {
                connection.disconnect();
              }
            }
          }
        });
  }
}

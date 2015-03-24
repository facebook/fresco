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
import com.facebook.imagepipeline.memory.ByteArrayPool;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.memory.PooledByteBufferFactory;

/**
 * Network producer using the simplest Android stack.
 *
 * <p>Apps requiring more sophisticated networking should implement their own
 * subclass of {@link NetworkFetchProducer}             .
 */
public class HttpURLConnectionNetworkFetchProducer extends NetworkFetchProducer<NfpRequestState> {
  private static final int NUM_NETWORK_THREADS = 3;

  private final Executor mExecutor;

  public HttpURLConnectionNetworkFetchProducer(
      PooledByteBufferFactory pooledByteBufferFactory,
      ByteArrayPool byteArrayPool) {
    super(pooledByteBufferFactory, byteArrayPool);
    mExecutor = Executors.newFixedThreadPool(NUM_NETWORK_THREADS);
  }

  @Override
  protected NfpRequestState newRequestState(
      Consumer<CloseableReference<PooledByteBuffer>> consumer,
      ProducerContext context) {
    return new NfpRequestState(consumer, context);
  }

  @Override
  protected void fetchImage(final NfpRequestState requestState) {
    mExecutor.execute(
        new Runnable() {
          @Override
          public void run() {
            HttpURLConnection connection = null;
            try {
              Uri uri = requestState.getUri();
              URL url = new URL(uri.toString());
              connection = (HttpURLConnection) url.openConnection();
              InputStream is = connection.getInputStream();
              processResult(requestState, is, 0, false);
            } catch (Exception e) {
              onFailure(requestState, e, null);
            } finally {
              if (connection != null) {
                connection.disconnect();
              }
            }
          }
        });
  }
}

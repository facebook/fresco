/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.backends.okhttp;

import java.io.IOException;

import android.net.Uri;

import com.facebook.common.logging.FLog;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.memory.ByteArrayPool;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.memory.PooledByteBufferFactory;
import com.facebook.imagepipeline.producers.BaseProducerContextCallbacks;
import com.facebook.imagepipeline.producers.Consumer;
import com.facebook.imagepipeline.producers.NfpRequestState;
import com.facebook.imagepipeline.producers.NetworkFetchProducer;
import com.facebook.imagepipeline.producers.ProducerContext;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.CacheControl;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

/**
 * Network fetch producer using OkHttp as a backend.
 *
 * <p> OkHttpNetworkFetchProducer supports request cancellation. This feature is enabled
 * via {@code cancellable} constructor parameter.
 */
public class OkHttpNetworkFetchProducer extends NetworkFetchProducer<NfpRequestState> {

  private static final String TAG = "OkHttpNetworkFetchProducer";

  private final OkHttpClient mOkHttpClient;
  private final boolean mCancellable;

  /**
   * @param okHttpClient client to use
   * @param cancellable whether to allow cancellation of submitted requests
   * @param pooledByteBufferFactory pooled byte buffer factory
   * @param byteArrayPool pool for stream input/ouptut buffering
   */
  public OkHttpNetworkFetchProducer(
      OkHttpClient okHttpClient,
      boolean cancellable,
      PooledByteBufferFactory pooledByteBufferFactory,
      ByteArrayPool byteArrayPool) {
    super(pooledByteBufferFactory, byteArrayPool);
    mOkHttpClient = okHttpClient;
    mCancellable = cancellable;
  }

  @Override
  protected NfpRequestState newRequestState(
      Consumer<CloseableReference<PooledByteBuffer>> consumer,
      ProducerContext context) {
    return new NfpRequestState(consumer, context);
  }

  @Override
  protected void fetchImage(final NfpRequestState requestState) {
    final Uri uri = requestState.getUri();
    final Request request = new Request.Builder()
        .cacheControl(new CacheControl.Builder().noStore().build())
        .url(uri.toString())
        .get()
        .build();
    final Call call = mOkHttpClient.newCall(request);

    if (mCancellable) {
      requestState.getContext().addCallbacks(
          new BaseProducerContextCallbacks() {
            @Override
            public void onCancellationRequested() {
              call.cancel();
            }
          });
    }

    call.enqueue(
        new Callback() {
          @Override
          public void onResponse(Response response) {
            final ResponseBody body = response.body();
            try {
              long contentLength = body.contentLength();
              if (contentLength < 0) {
                contentLength = 0;
              }
              processResult(requestState, body.byteStream(), (int) contentLength, false);
            } catch (IOException ioe) {
              handleException(call, requestState, ioe);
            } finally {
              try {
                body.close();
              } catch (IOException ioe) {
                FLog.w(TAG, "Exception when closing response body", ioe);
              }
            }
          }

          @Override
          public void onFailure(final Request request, final IOException e) {
            handleException(call, requestState, e);
          }
        });
  }

  /**
   * Handles IOExceptions.
   *
   * <p> OkHttp notifies callers of cancellations via an IOException. If IOException is caught
   * after request cancellation, then the exception is interpreted as successful cancellation
   * and onCancellation is called. Otherwise onFailure is called.
   */
  private void handleException(
      final Call call,
      final NfpRequestState requestState,
      final IOException ioe) {
    if (call.isCanceled()) {
      onCancellation(requestState, null);
    } else {
      onFailure(requestState, ioe, null);
    }
  }
}

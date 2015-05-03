/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.backends.okhttp;

import android.net.Uri;
import android.os.Looper;

import com.facebook.common.logging.FLog;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.producers.BaseNetworkFetcher;
import com.facebook.imagepipeline.producers.BaseProducerContextCallbacks;
import com.facebook.imagepipeline.producers.Consumer;
import com.facebook.imagepipeline.producers.FetchState;
import com.facebook.imagepipeline.producers.ProducerContext;
import com.squareup.okhttp.CacheControl;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

import java.io.IOException;
import java.util.concurrent.Executor;

/**
 * Network fetcher that uses OkHttp as a backend.
 */
public class OkHttpNetworkFetcher extends BaseNetworkFetcher<FetchState> {

  private static final String TAG = "OkHttpNetworkFetchProducer";

  private final OkHttpClient mOkHttpClient;

  private Executor mCancellationExecutor;

  /**
   * @param okHttpClient client to use
   */
  public OkHttpNetworkFetcher(OkHttpClient okHttpClient) {
    mOkHttpClient = okHttpClient;
    mCancellationExecutor = okHttpClient.getDispatcher().getExecutorService();
  }

  @Override
  public FetchState createFetchState(
      Consumer<CloseableReference<PooledByteBuffer>> consumer,
      ProducerContext context) {
    return new FetchState(consumer, context);
  }

  @Override
  public void fetch(final FetchState requestState, final Callback callback) {
    final Uri uri = requestState.getUri();
    final Request request = new Request.Builder()
        .cacheControl(new CacheControl.Builder().noStore().build())
        .url(uri.toString())
        .get()
        .build();
    final Call call = mOkHttpClient.newCall(request);

    requestState.getContext().addCallbacks(
        new BaseProducerContextCallbacks() {
          @Override
          public void onCancellationRequested() {
            if (Looper.myLooper() != Looper.getMainLooper()) {
              call.cancel();
            } else {
              mCancellationExecutor.execute(new Runnable() {
                @Override public void run() {
                  call.cancel();
                }
              });
            }
          }
        });

    call.enqueue(
        new com.squareup.okhttp.Callback() {
          @Override
          public void onResponse(Response response) {
            final ResponseBody body = response.body();
            try {
              long contentLength = body.contentLength();
              if (contentLength < 0) {
                contentLength = 0;
              }
              callback.onResponse(body.byteStream(), (int) contentLength);
            } catch (IOException ioe) {
              handleException(call, ioe, callback);
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
            handleException(call, e, callback);
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
  private void handleException(final Call call, final IOException ioe, final Callback callback) {
    if (call.isCanceled()) {
      callback.onCancellation();
    } else {
      callback.onFailure(ioe);
    }
  }
}

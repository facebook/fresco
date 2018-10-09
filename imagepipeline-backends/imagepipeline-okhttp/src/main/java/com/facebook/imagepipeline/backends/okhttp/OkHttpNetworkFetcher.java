/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.backends.okhttp;

import android.net.Uri;
import android.os.Looper;
import android.os.SystemClock;
import com.facebook.common.logging.FLog;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.producers.BaseNetworkFetcher;
import com.facebook.imagepipeline.producers.BaseProducerContextCallbacks;
import com.facebook.imagepipeline.producers.Consumer;
import com.facebook.imagepipeline.producers.FetchState;
import com.facebook.imagepipeline.producers.NetworkFetcher;
import com.facebook.imagepipeline.producers.ProducerContext;
import com.squareup.okhttp.CacheControl;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;

/**
 * Network fetcher that uses OkHttp as a backend.
 *
 * @deprecated replaced with {@code
 * com.facebook.imagepipeline.backends.okhttp3.OkHttpNetworkFetcher}.
 */
public class OkHttpNetworkFetcher extends
    BaseNetworkFetcher<OkHttpNetworkFetcher.OkHttpNetworkFetchState> {

  public static class OkHttpNetworkFetchState extends FetchState {
    public long submitTime;
    public long responseTime;
    public long fetchCompleteTime;

    public OkHttpNetworkFetchState(
        Consumer<EncodedImage> consumer,
        ProducerContext producerContext) {
      super(consumer, producerContext);
    }
  }

  private static final String TAG = "OkHttpNetworkFetchProducer";
  private static final String QUEUE_TIME = "queue_time";
  private static final String FETCH_TIME = "fetch_time";
  private static final String TOTAL_TIME = "total_time";
  private static final String IMAGE_SIZE = "image_size";

  private final OkHttpClient mOkHttpClient;
  private final @Nullable CacheControl mCacheControl;

  private Executor mCancellationExecutor;

  /**
   * @param okHttpClient client to use
   */
  public OkHttpNetworkFetcher(OkHttpClient okHttpClient) {
    this(okHttpClient, true);
  }

  /**
   * @param okHttpClient client to use
   * @param disableOkHttpCache true if network requests should not be cached by OkHttp
   */
  public OkHttpNetworkFetcher(OkHttpClient okHttpClient, boolean disableOkHttpCache) {
    mOkHttpClient = okHttpClient;
    mCancellationExecutor = okHttpClient.getDispatcher().getExecutorService();
    mCacheControl = disableOkHttpCache ? new CacheControl.Builder().noStore().build() : null;
  }

  @Override
  public OkHttpNetworkFetchState createFetchState(
      Consumer<EncodedImage> consumer,
      ProducerContext context) {
    return new OkHttpNetworkFetchState(consumer, context);
  }

  @Override
  public void fetch(
      final OkHttpNetworkFetchState fetchState, final NetworkFetcher.Callback callback) {
    fetchState.submitTime = SystemClock.uptimeMillis();
    final Uri uri = fetchState.getUri();

    try {
      final Request.Builder builder = new Request.Builder().url(uri.toString()).get();
      if (mCacheControl != null) {
        builder.cacheControl(mCacheControl);
      }
      fetchWithRequest(fetchState, callback, builder.build());
    } catch (Exception e) {
      // handle error while creating the request
      callback.onFailure(e);
    }
  }

  protected void fetchWithRequest(
      final OkHttpNetworkFetchState fetchState,
      final NetworkFetcher.Callback callback,
      final Request request) {

    final Call call = mOkHttpClient.newCall(request);

    fetchState.getContext().addCallbacks(
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
            fetchState.responseTime = SystemClock.uptimeMillis();
            final ResponseBody body = response.body();
            try {
              if (!response.isSuccessful()) {
                handleException(
                        call,
                        new IOException("Unexpected HTTP code " + response),
                        callback);
                return;
              }

              long contentLength = body.contentLength();
              if (contentLength < 0) {
                contentLength = 0;
              }
              callback.onResponse(body.byteStream(), (int) contentLength);
            } catch (Exception e) {
              handleException(call, e, callback);
            } finally {
              try {
                body.close();
              } catch (Exception e) {
                FLog.w(TAG, "Exception when closing response body", e);
              }
            }
          }

          @Override
          public void onFailure(final Request request, final IOException e) {
            handleException(call, e, callback);
          }
        });
  }

  @Override
  public void onFetchCompletion(OkHttpNetworkFetchState fetchState, int byteSize) {
    fetchState.fetchCompleteTime = SystemClock.uptimeMillis();
  }

  @Override
  public Map<String, String> getExtraMap(OkHttpNetworkFetchState fetchState, int byteSize) {
    Map<String, String> extraMap = new HashMap<>(4);
    extraMap.put(QUEUE_TIME, Long.toString(fetchState.responseTime - fetchState.submitTime));
    extraMap.put(FETCH_TIME, Long.toString(fetchState.fetchCompleteTime - fetchState.responseTime));
    extraMap.put(TOTAL_TIME, Long.toString(fetchState.fetchCompleteTime - fetchState.submitTime));
    extraMap.put(IMAGE_SIZE, Integer.toString(byteSize));
    return extraMap;
  }

  /**
   * Handles exceptions.
   *
   * <p> OkHttp notifies callers of cancellations via an IOException. If IOException is caught
   * after request cancellation, then the exception is interpreted as successful cancellation
   * and onCancellation is called. Otherwise onFailure is called.
   */
  private void handleException(final Call call, final Exception e, final Callback callback) {
    if (call.isCanceled()) {
      callback.onCancellation();
    } else {
      callback.onFailure(e);
    }
  }
}

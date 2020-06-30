/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import android.net.Uri;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.time.MonotonicClock;
import com.facebook.common.time.RealtimeSinceBootClock;
import com.facebook.common.util.UriUtil;
import com.facebook.imagepipeline.image.EncodedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.annotation.Nullable;

/**
 * Network fetcher that uses the simplest Android stack.
 *
 * <p>Apps requiring more sophisticated networking should implement their own {@link
 * NetworkFetcher}.
 */
public class HttpUrlConnectionNetworkFetcher
    extends BaseNetworkFetcher<HttpUrlConnectionNetworkFetcher.HttpUrlConnectionNetworkFetchState> {

  public static class HttpUrlConnectionNetworkFetchState extends FetchState {

    private long submitTime;
    private long responseTime;
    private long fetchCompleteTime;

    public HttpUrlConnectionNetworkFetchState(
        Consumer<EncodedImage> consumer, ProducerContext producerContext) {
      super(consumer, producerContext);
    }
  }

  private static final String QUEUE_TIME = "queue_time";
  private static final String FETCH_TIME = "fetch_time";
  private static final String TOTAL_TIME = "total_time";
  private static final String IMAGE_SIZE = "image_size";

  private static final int NUM_NETWORK_THREADS = 3;
  private static final int MAX_REDIRECTS = 5;

  public static final int HTTP_TEMPORARY_REDIRECT = 307;
  public static final int HTTP_PERMANENT_REDIRECT = 308;

  public static final int HTTP_DEFAULT_TIMEOUT = 30000;

  private int mHttpConnectionTimeout;
  @Nullable private String mUserAgent;
  @Nullable private final Map<String, String> mRequestHeaders;

  private final ExecutorService mExecutorService;
  private final MonotonicClock mMonotonicClock;

  public HttpUrlConnectionNetworkFetcher() {
    this(null, null, RealtimeSinceBootClock.get());
  }

  public HttpUrlConnectionNetworkFetcher(int httpConnectionTimeout) {
    this(null, null, RealtimeSinceBootClock.get());
    mHttpConnectionTimeout = httpConnectionTimeout;
  }

  public HttpUrlConnectionNetworkFetcher(String userAgent, int httpConnectionTimeout) {
    this(userAgent, null, RealtimeSinceBootClock.get());
    mHttpConnectionTimeout = httpConnectionTimeout;
  }

  public HttpUrlConnectionNetworkFetcher(
      String userAgent, @Nullable Map<String, String> requestHeaders, int httpConnectionTimeout) {
    this(userAgent, requestHeaders, RealtimeSinceBootClock.get());
    mHttpConnectionTimeout = httpConnectionTimeout;
  }

  @VisibleForTesting
  HttpUrlConnectionNetworkFetcher(
      @Nullable String userAgent,
      @Nullable Map<String, String> requestHeaders,
      MonotonicClock monotonicClock) {
    mExecutorService = Executors.newFixedThreadPool(NUM_NETWORK_THREADS);
    mMonotonicClock = monotonicClock;
    mRequestHeaders = requestHeaders;
    mUserAgent = userAgent;
  }

  @Override
  public HttpUrlConnectionNetworkFetchState createFetchState(
      Consumer<EncodedImage> consumer, ProducerContext context) {
    return new HttpUrlConnectionNetworkFetchState(consumer, context);
  }

  @Override
  public void fetch(final HttpUrlConnectionNetworkFetchState fetchState, final Callback callback) {
    fetchState.submitTime = mMonotonicClock.now();
    final Future<?> future =
        mExecutorService.submit(
            new Runnable() {
              @Override
              public void run() {
                fetchSync(fetchState, callback);
              }
            });
    fetchState
        .getContext()
        .addCallbacks(
            new BaseProducerContextCallbacks() {
              @Override
              public void onCancellationRequested() {
                if (future.cancel(false)) {
                  callback.onCancellation();
                }
              }
            });
  }

  @VisibleForTesting
  void fetchSync(HttpUrlConnectionNetworkFetchState fetchState, Callback callback) {
    HttpURLConnection connection = null;
    InputStream is = null;
    try {
      connection = downloadFrom(fetchState.getUri(), MAX_REDIRECTS);
      fetchState.responseTime = mMonotonicClock.now();

      if (connection != null) {
        is = connection.getInputStream();
        callback.onResponse(is, -1);
      }
    } catch (IOException e) {
      callback.onFailure(e);
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException e) {
          // do nothing and ignore the IOException here
        }
      }
      if (connection != null) {
        connection.disconnect();
      }
    }
  }

  private HttpURLConnection downloadFrom(Uri uri, int maxRedirects) throws IOException {
    HttpURLConnection connection = openConnectionTo(uri);
    if (mUserAgent != null) {
      connection.setRequestProperty("User-Agent", mUserAgent);
    }
    if (mRequestHeaders != null) {
      for (Map.Entry<String, String> entry : mRequestHeaders.entrySet()) {
        connection.setRequestProperty(entry.getKey(), entry.getValue());
      }
    }
    connection.setConnectTimeout(mHttpConnectionTimeout);
    int responseCode = connection.getResponseCode();

    if (isHttpSuccess(responseCode)) {
      return connection;

    } else if (isHttpRedirect(responseCode)) {
      String nextUriString = connection.getHeaderField("Location");
      connection.disconnect();

      Uri nextUri = (nextUriString == null) ? null : Uri.parse(nextUriString);
      String originalScheme = uri.getScheme();

      if (maxRedirects > 0 && nextUri != null && !nextUri.getScheme().equals(originalScheme)) {
        return downloadFrom(nextUri, maxRedirects - 1);
      } else {
        String message =
            maxRedirects == 0
                ? error("URL %s follows too many redirects", uri.toString())
                : error(
                    "URL %s returned %d without a valid redirect", uri.toString(), responseCode);
        throw new IOException(message);
      }

    } else {
      connection.disconnect();
      throw new IOException(
          String.format("Image URL %s returned HTTP code %d", uri.toString(), responseCode));
    }
  }

  @VisibleForTesting
  static HttpURLConnection openConnectionTo(Uri uri) throws IOException {
    URL url = UriUtil.uriToUrl(uri);
    return (HttpURLConnection) url.openConnection();
  }

  @Override
  public void onFetchCompletion(HttpUrlConnectionNetworkFetchState fetchState, int byteSize) {
    fetchState.fetchCompleteTime = mMonotonicClock.now();
  }

  private static boolean isHttpSuccess(int responseCode) {
    return (responseCode >= HttpURLConnection.HTTP_OK
        && responseCode < HttpURLConnection.HTTP_MULT_CHOICE);
  }

  private static boolean isHttpRedirect(int responseCode) {
    switch (responseCode) {
      case HttpURLConnection.HTTP_MULT_CHOICE:
      case HttpURLConnection.HTTP_MOVED_PERM:
      case HttpURLConnection.HTTP_MOVED_TEMP:
      case HttpURLConnection.HTTP_SEE_OTHER:
      case HTTP_TEMPORARY_REDIRECT:
      case HTTP_PERMANENT_REDIRECT:
        return true;
      default:
        return false;
    }
  }

  private static String error(String format, Object... args) {
    return String.format(Locale.getDefault(), format, args);
  }

  @Override
  public Map<String, String> getExtraMap(
      HttpUrlConnectionNetworkFetchState fetchState, int byteSize) {
    Map<String, String> extraMap = new HashMap<>(4);
    extraMap.put(QUEUE_TIME, Long.toString(fetchState.responseTime - fetchState.submitTime));
    extraMap.put(FETCH_TIME, Long.toString(fetchState.fetchCompleteTime - fetchState.responseTime));
    extraMap.put(TOTAL_TIME, Long.toString(fetchState.fetchCompleteTime - fetchState.submitTime));
    extraMap.put(IMAGE_SIZE, Integer.toString(byteSize));
    return extraMap;
  }
}

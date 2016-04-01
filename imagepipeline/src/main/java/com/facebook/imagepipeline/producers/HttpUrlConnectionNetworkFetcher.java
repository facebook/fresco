/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import android.net.Uri;

import com.facebook.imagepipeline.image.EncodedImage;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Network fetcher that uses the simplest Android stack.
 *
 * <p> Apps requiring more sophisticated networking should implement their own
 * {@link NetworkFetcher}.
 */
public class HttpUrlConnectionNetworkFetcher extends BaseNetworkFetcher<FetchState> {

  private final static int HTTP_CONNECT_TIMEOUT = 4000;//4s
  private final static int HTTP_READ_TIMEOUT = 10000;//10s
  protected static final int MAX_REDIRECT_COUNT = 5;
    protected static final String ALLOWED_URI_CHARS = "@#&=*+-_.,:!?()/~'%;";

  public HttpUrlConnectionNetworkFetcher() {
  }

  @Override
  public FetchState createFetchState(Consumer<EncodedImage> consumer, ProducerContext context) {
    return new FetchState(consumer, context);
  }

  @Override
  public void fetch(final FetchState fetchState, final Callback callback) {
	  HttpURLConnection connection = null;
      Uri uri = fetchState.getUri();
      String uriString = uri.toString();

      InputStream is;
      try {
          connection = createGetConnection(uriString);
          int redirectCount = 0;
          while (connection.getResponseCode() / 100 == 3 && redirectCount < MAX_REDIRECT_COUNT) {
              uriString = connection.getHeaderField("Location");
              connection.disconnect();
              connection = createGetConnection(uriString);
              ++redirectCount;
          }
          if (redirectCount > MAX_REDIRECT_COUNT) {
              callback.onFailure(new Exception("RedirectCount more than " + MAX_REDIRECT_COUNT));
              return;
          }
          is = connection.getInputStream();
          callback.onResponse(is, -1);//connection.getContentLength()
      } catch (Exception e) {
            callback.onFailure(e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
      }
  }

  protected HttpURLConnection createGetConnection(String uriString) throws IOException {
        String encodedUrl = Uri.encode(uriString, ALLOWED_URI_CHARS);
        URL url = new URL(encodedUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(HTTP_CONNECT_TIMEOUT);
        connection.setReadTimeout(HTTP_READ_TIMEOUT);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod("GET");
        return connection;
  }

    protected HttpURLConnection createHeadConnection(String uriString) throws IOException {
        URL url = new URL(uriString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(HTTP_CONNECT_TIMEOUT);
        connection.setReadTimeout(HTTP_READ_TIMEOUT);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod("HEAD");
        return connection;
    }
}

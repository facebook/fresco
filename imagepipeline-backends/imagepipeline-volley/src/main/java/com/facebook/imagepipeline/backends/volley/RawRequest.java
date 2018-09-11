/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.backends.volley;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.HttpHeaderParser;

/**
 * A Volley request that will return the raw content as a byte array and does not use
 * Volley's cache.
 */
public class RawRequest extends Request<byte[]> {

  private final Listener<byte[]> mListener;

  public RawRequest(String url, Listener<byte[]> listener, ErrorListener errorListener) {
    super(0, url, errorListener);
    this.mListener = listener;
    setShouldCache(false);
  }

  @Override
  protected Response<byte[]> parseNetworkResponse(NetworkResponse response) {
    return Response.success(response.data, HttpHeaderParser.parseCacheHeaders(response));
  }

  @Override
  protected void deliverResponse(byte[] bytes) {
    mListener.onResponse(bytes);
  }
}

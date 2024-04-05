/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.backends.okhttp3

import okhttp3.Headers
import okhttp3.Response

class OkHttpNetworkFetcherException() : Exception() {
  private var responseCode: Int? = null
  private var responseHeaders: Headers? = null

  companion object {
    @JvmStatic
    fun fromResponse(response: Response): OkHttpNetworkFetcherException {
      val ex = OkHttpNetworkFetcherException()
      ex.responseCode = response.networkResponse()?.code();
      ex.responseHeaders = response.networkResponse()?.headers();
      return ex;
    }
  }
}

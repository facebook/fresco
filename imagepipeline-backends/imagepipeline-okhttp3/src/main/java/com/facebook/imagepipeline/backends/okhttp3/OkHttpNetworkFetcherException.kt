/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.backends.okhttp3

import okhttp3.Headers
import okhttp3.Response

class OkHttpNetworkFetcherException(
    val responseCode: Int? = null,
    val responseHeaders: Headers? = null,
) : Exception() {

  companion object {
    @JvmStatic
    fun fromResponse(response: Response): OkHttpNetworkFetcherException =
        OkHttpNetworkFetcherException(
            response.networkResponse()?.code(), response.networkResponse()?.headers())
  }
}

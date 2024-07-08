/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.ui.common

import android.net.Uri
import com.facebook.common.internal.Fn

object MultiUriHelper {
  @JvmStatic
  fun <T> getMainUri(
      mainRequest: T?,
      lowResRequest: T?,
      firstAvailableRequest: Array<T?>?,
      requestToUri: Fn<T, Uri?>
  ): Uri? {

    val uri = mainRequest?.let(requestToUri::apply)
    if (uri != null) {
      return uri
    }

    if (!firstAvailableRequest.isNullOrEmpty()) {
      val firstUri = firstAvailableRequest[0]?.let(requestToUri::apply)
      if (firstUri != null) {
        return firstUri
      }
    }

    return lowResRequest?.let(requestToUri::apply)
  }
}

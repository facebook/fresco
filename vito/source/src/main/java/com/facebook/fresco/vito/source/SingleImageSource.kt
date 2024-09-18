/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.source

import android.net.Uri

@Suppress("KtDataClass")
interface SingleImageSource : UriImageSource {
  val uri: Uri

  fun getStringExtra(key: String): String?
}

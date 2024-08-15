// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.fresco.vito.source

import android.net.Uri

interface UriImageSource : ImageSource {
  val imageUri: Uri

  val extras: Map<String, Any>?
}

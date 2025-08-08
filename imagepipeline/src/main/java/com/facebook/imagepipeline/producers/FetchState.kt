/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers

import android.net.Uri
import com.facebook.imagepipeline.common.BytesRange
import com.facebook.imagepipeline.image.EncodedImage

/**
 * Used by [NetworkFetcher] to encapsulate the state of one network fetch.
 *
 * Implementations can subclass this to store additional fetch-scoped fields.
 */
open class FetchState(val consumer: Consumer<EncodedImage?>, val context: ProducerContext) {
  var lastIntermediateResultTimeMs: Long = 0

  /**
   * EXPERIMENTAL: Allows the fetcher to set extra status flags to be included in calls to
   * [ ][Consumer.onNewResult].
   */
  @get:Consumer.Status var onNewResultStatusFlags: Int = 0

  /**
   * EXPERIMENTAL: Allows the fetcher to identify that the reponse is for an imcomplete portion of
   * the whole image by defining the range of bytes being provided.
   */
  var responseBytesRange: BytesRange? = null

  val id: String
    get() = context.id

  val listener: ProducerListener2
    get() = context.producerListener

  open val uri: Uri
    get() = context.imageRequest.sourceUri

  open val query: String?
    get() = uri.query
}

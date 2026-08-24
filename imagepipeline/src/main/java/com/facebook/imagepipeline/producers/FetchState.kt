/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers

import android.net.Uri
import com.facebook.imageformat.ImageFormat
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
   * EXPERIMENTAL: Allows the fetcher to identify that the response is for an incomplete portion of
   * the whole image by defining the range of bytes being provided.
   */
  var responseBytesRange: BytesRange? = null

  /**
   * Format of the response body, sniffed from its first bytes once they arrive, and null before
   * that or for a body no registered checker recognises. Set by fetchers that identify the body
   * before handing it on, so that decisions which are format-specific — whether partial results are
   * worth propagating, in particular — can be made from what actually came back rather than from
   * the URL that asked for it.
   */
  @Volatile var responseImageFormat: ImageFormat? = null

  val id: String
    get() = context.id

  val listener: ProducerListener2
    get() = context.producerListener

  open val uri: Uri
    get() = context.imageRequest.sourceUri

  open val query: String?
    get() = uri.query
}

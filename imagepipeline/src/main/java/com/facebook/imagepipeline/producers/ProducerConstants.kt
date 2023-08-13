/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers

/**
 * Constants to be used various [Producer]s for logging purposes in the extra maps for the
 * [com.facebook.imagepipeline.listener.RequestListener].
 *
 * The elements are package visible on purpose such that the individual producers create public
 * constants of the ones that they actually use.
 */
internal object ProducerConstants {
  const val EXTRA_CACHED_VALUE_FOUND = "cached_value_found"
  const val EXTRA_BITMAP_SIZE = "bitmapSize"
  const val EXTRA_HAS_GOOD_QUALITY = "hasGoodQuality"
  const val EXTRA_IS_FINAL = "isFinal"
  const val EXTRA_IMAGE_FORMAT_NAME = "imageFormat"
  const val EXTRA_BYTES = "byteCount"
  const val ENCODED_IMAGE_SIZE = "encodedImageSize"
  const val REQUESTED_IMAGE_SIZE = "requestedImageSize"
  const val SAMPLE_SIZE = "sampleSize"
  const val NON_FATAL_DECODE_ERROR = "non_fatal_decode_error"
}

/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.middleware

typealias Extras = Map<String, Any?>

interface HasExtraData {

  fun <E> putExtra(key: String, value: E?)

  fun <E> getExtra(key: String): E?

  fun <E> getExtra(key: String, valueIfNotFound: E? = null): E?

  fun getExtras(): Extras

  fun putExtras(extras: Extras)

  companion object {
    const val KEY_ENCODED_SIZE = "encoded_size"
    const val KEY_ENCODED_WIDTH = "encoded_width"
    const val KEY_ENCODED_HEIGHT = "encoded_height"
    const val KEY_URI_SOURCE = "uri_source"
    const val KEY_IMAGE_FORMAT = "image_format"
    const val KEY_BITMAP_CONFIG = "bitmap_config"
    const val KEY_IS_ROUNDED = "is_rounded"
    const val KEY_NON_FATAL_DECODE_ERROR = "non_fatal_decode_error"
    const val KEY_MODIFIED_URL = "modified_url"

    const val KEY_ORIGIN = "origin"
    const val KEY_ORIGIN_SUBCATEGORY = "origin_sub"
    const val KEY_SOURCE_URI = "uri_source"
    const val KEY_NORMALIZED_URI = "uri_norm"

    /* number of deduped request in BitmapMemoryCacheKeyMultiplexProducer */
    const val KEY_MULTIPLEX_BITMAP_COUNT = "multiplex_bmp_cnt"
    /* number of deduped request in EncodedCacheKeyMultiplexProducer */
    const val KEY_MULTIPLEX_ENCODED_COUNT = "multiplex_enc_cnt"
    const val KEY_LAST_SCAN_NUMBER = "last_scan_num"

    const val KEY_IMAGE_SOURCE_EXTRAS = "image_source_extras"
  }
}

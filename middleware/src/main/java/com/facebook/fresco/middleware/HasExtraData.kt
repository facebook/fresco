/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.middleware

typealias Extras = Map<String, Any>

interface HasExtraData {

  fun <E> putExtra(key: String, value: E?)

  fun <E> getExtra(key: String): E?

  fun <E> getExtra(key: String, valueIfNotFound: E? = null): E?

  fun getExtras(): Extras

  fun putExtras(extras: Extras)

  companion object {
    const val KEY_ID = "id"
    const val KEY_ENCODED_SIZE = "encoded_size"
    const val KEY_ENCODED_WIDTH = "encoded_width"
    const val KEY_ENCODED_HEIGHT = "encoded_height"
    const val KEY_URI_SOURCE = "uri_source"
    const val KEY_IMAGE_FORMAT = "image_format"
    const val KEY_BITMAP_CONFIG = "bitmap_config"
    const val KEY_IS_ROUNDED = "is_rounded"
    const val KEY_NON_FATAL_DECODE_ERROR = "non_fatal_decode_error"

    /** The original URL if it was modified by Dynamic Image URL */
    const val KEY_SF_ORIGINAL_URL = "smart_original_url"

    /** Information on if/why the url was modified by Dynamic Image URL */
    const val KEY_SF_FETCH_STRATEGY = "smart_fetch_strategy"
    const val KEY_SF_MOD_RESULT = "smart_mod_result"
    const val KEY_SF_ADAPTIVE = "smart_adaptive"
    const val KEY_SF_VARIATION = "smart_variation"
    const val KEY_IMAGE_SOURCE_TYPE: String = "image_source_type"

    const val KEY_ORIGIN = "origin"
    const val KEY_ORIGIN_SUBCATEGORY = "origin_sub"

    /* number of deduped request in BitmapMemoryCacheKeyMultiplexProducer */
    const val KEY_MULTIPLEX_BITMAP_COUNT = "multiplex_bmp_cnt"

    /* number of deduped request in EncodedCacheKeyMultiplexProducer */
    const val KEY_MULTIPLEX_ENCODED_COUNT = "multiplex_enc_cnt"
    const val KEY_LAST_SCAN_NUMBER = "last_scan_num"
    const val TARGET_SCAN = "target_scan"

    const val KEY_IMAGE_SOURCE_EXTRAS = "image_source_extras"
    const val KEY_COLOR_SPACE = "image_color_space"

    const val KEY_VIEWPORT = "viewport"
    const val KEY_SCALETYPE = "scaletype"
    const val KEY_SIZING_HINT = "sizing_hint"
    const val KEY_IMAGEOPTIONS = "imageoptions"

    /* HDR related image extra data */
    const val STORED_IMAGE_HAS_GAIN_MAP = "stored_image_has_gain_map"
    const val FETCHED_IMAGE_HAS_GAIN_MAP = "fetched_image_has_gain_map"
    const val IS_HDR = "is_HDR"
  }
}

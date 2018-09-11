/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

/**
 * Constants to be used various {@link Producer}s for logging purposes in the extra maps for the
 * {@link com.facebook.imagepipeline.listener.RequestListener}.
 *
 * The elements are package visible on purpose such that the individual producers create public
 * constants of the ones that they actually use.
 */
class ProducerConstants {

  static final String EXTRA_CACHED_VALUE_FOUND = "cached_value_found";

  static final String EXTRA_BITMAP_SIZE = "bitmapSize";
  static final String EXTRA_HAS_GOOD_QUALITY = "hasGoodQuality";
  static final String EXTRA_IMAGE_TYPE = "imageType";
  static final String EXTRA_IS_FINAL = "isFinal";
  static final String EXTRA_IMAGE_FORMAT_NAME = "imageFormat";
  static final String ENCODED_IMAGE_SIZE = "encodedImageSize";
  static final String REQUESTED_IMAGE_SIZE = "requestedImageSize";
  static final String SAMPLE_SIZE = "sampleSize";
}

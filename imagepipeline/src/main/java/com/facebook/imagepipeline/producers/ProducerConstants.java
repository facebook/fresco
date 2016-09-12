/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
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
}

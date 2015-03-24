/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.animated.impl;

import com.facebook.imagepipeline.animated.base.AnimatedDrawableBackend;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableOptions;

/**
 * Assisted provider for {@link AnimatedDrawableCachingBackendImpl}.
 */
public interface AnimatedDrawableCachingBackendImplProvider {

  /**
   * Creates a new {@link AnimatedDrawableCachingBackendImpl}.
   *
   * @param animatedDrawableBackend the backend to delegate to
   * @param options the options for the drawable
   * @return a new {@link AnimatedDrawableCachingBackendImpl}
   */
  AnimatedDrawableCachingBackendImpl get(
      AnimatedDrawableBackend animatedDrawableBackend,
      AnimatedDrawableOptions options);
}

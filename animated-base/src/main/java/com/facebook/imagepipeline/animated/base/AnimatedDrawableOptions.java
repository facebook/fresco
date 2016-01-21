/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.animated.base;

import javax.annotation.concurrent.Immutable;

/**
 * Options for creating {@link AnimatableDrawable}.
 */
@Immutable
public class AnimatedDrawableOptions {

  /**
   * Default options.
   */
  public static AnimatedDrawableOptions DEFAULTS = AnimatedDrawableOptions.newBuilder().build();

  /**
   * Whether all the rendered frames should be held in memory disregarding other constraints.
   */
  public final boolean forceKeepAllFramesInMemory;

  /**
   * Whether the drawable can use worker threads to optimistically prefetch frames.
   */
  public final boolean allowPrefetching;

  /**
   * The maximum bytes that the backend can use to cache image frames in memory or -1
   * to use the default
   */
  public final int maximumBytes;

  /**
   * Whether to enable additional verbose debugging diagnostics.
   */
  public final boolean enableDebugging;

  /**
   * Creates {@link AnimatedDrawableOptions} with default options.
   */
  public AnimatedDrawableOptions(AnimatedDrawableOptionsBuilder builder) {
    this.forceKeepAllFramesInMemory = builder.getForceKeepAllFramesInMemory();
    this.allowPrefetching = builder.getAllowPrefetching();
    this.maximumBytes = builder.getMaximumBytes();
    this.enableDebugging = builder.getEnableDebugging();
  }

  /**
   * Creates a new builder.
   *
   * @return the builder
   */
  public static AnimatedDrawableOptionsBuilder newBuilder() {
    return new AnimatedDrawableOptionsBuilder();
  }
}

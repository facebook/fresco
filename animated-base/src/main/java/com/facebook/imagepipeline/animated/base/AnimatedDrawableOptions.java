/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.animated.base;

import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.concurrent.Immutable;

/** Options for creating {@link com.facebook.fresco.animation.drawable.AnimatedDrawable2}. */
@Nullsafe(Nullsafe.Mode.STRICT)
@Immutable
public class AnimatedDrawableOptions {

  /** Default options. */
  public static AnimatedDrawableOptions DEFAULTS = AnimatedDrawableOptions.newBuilder().build();

  /** Whether all the rendered frames should be held in memory disregarding other constraints. */
  public final boolean forceKeepAllFramesInMemory;

  /** Whether the drawable can use worker threads to optimistically prefetch frames. */
  public final boolean allowPrefetching;

  /**
   * The maximum bytes that the backend can use to cache image frames in memory or -1 to use the
   * default
   */
  public final int maximumBytes;

  /** Whether to enable additional verbose debugging diagnostics. */
  public final boolean enableDebugging;

  /** Creates {@link AnimatedDrawableOptions} with default options. */
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

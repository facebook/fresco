/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.animated.base;

import com.facebook.infer.annotation.Nullsafe;

/** Builder for {@link AnimatedDrawableOptions}. */
@Nullsafe(Nullsafe.Mode.STRICT)
public class AnimatedDrawableOptionsBuilder {

  private boolean mForceKeepAllFramesInMemory;
  private boolean mAllowPrefetching = true;
  private int mMaximumBytes = -1;
  private boolean mEnableDebugging;

  /**
   * Gets whether all the rendered frames should be held in memory disregarding other constraints.
   *
   * @return whether all the rendered frames should be held in memory
   */
  public boolean getForceKeepAllFramesInMemory() {
    return mForceKeepAllFramesInMemory;
  }

  /**
   * Sets whether all the rendered frames should be held in memory disregarding other constraints.
   *
   * @param forceKeepAllFramesInMemory whether to force the frames to be held in memory
   * @return this builder
   */
  public AnimatedDrawableOptionsBuilder setForceKeepAllFramesInMemory(
      boolean forceKeepAllFramesInMemory) {
    mForceKeepAllFramesInMemory = forceKeepAllFramesInMemory;
    return this;
  }

  /**
   * Gets whether the drawable can use worker threads to optimistically prefetch frames.
   *
   * @return whether the backend can use worker threads to prefetch frames
   */
  public boolean getAllowPrefetching() {
    return mAllowPrefetching;
  }

  /**
   * Sets whether the drawable can use worker threads to optimistically prefetch frames.
   *
   * @param allowPrefetching whether the backend can use worker threads to prefetch frames
   * @return this builder
   */
  public AnimatedDrawableOptionsBuilder setAllowPrefetching(boolean allowPrefetching) {
    mAllowPrefetching = allowPrefetching;
    return this;
  }

  /**
   * Gets the maximum bytes that the backend can use to cache image frames in memory.
   *
   * @return maximumBytes maximum bytes that the backend can use to cache image frames in memory or
   *     -1 to use the default
   */
  public int getMaximumBytes() {
    return mMaximumBytes;
  }

  /**
   * Sets the maximum bytes that the backend can use to cache image frames in memory.
   *
   * @param maximumBytes maximum bytes that the backend can use to cache image frames in memory or
   *     -1 to use the default
   * @return this builder
   */
  public AnimatedDrawableOptionsBuilder setMaximumBytes(int maximumBytes) {
    mMaximumBytes = maximumBytes;
    return this;
  }

  /**
   * Gets whether to enable additional verbose debugging diagnostics.
   *
   * @return whether to enable additional verbose debugging diagnostics
   */
  public boolean getEnableDebugging() {
    return mEnableDebugging;
  }

  /**
   * Sets whether to enable additional verbose debugging diagnostics.
   *
   * @param enableDebugging whether to enable additional verbose debugging diagnostics
   * @return this builder
   */
  public AnimatedDrawableOptionsBuilder setEnableDebugging(boolean enableDebugging) {
    mEnableDebugging = enableDebugging;
    return this;
  }

  /**
   * Builds the immutable options instance.
   *
   * @return the options instance
   */
  public AnimatedDrawableOptions build() {
    return new AnimatedDrawableOptions(this);
  }
}

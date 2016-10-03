/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.drawee.backends.pipeline;

import javax.annotation.Nullable;

import com.facebook.common.internal.ImmutableList;

import java.util.ArrayList;
import java.util.List;

/**
 * Drawee configuration.
 */
public class DraweeConfig {

  @Nullable
  private final ImmutableList<DrawableFactory> mCustomDrawableFactories;
  private final boolean mDrawDebugOverlay;

  private DraweeConfig(Builder builder) {
    mCustomDrawableFactories = builder.mCustomDrawableFactories != null
        ? ImmutableList.copyOf(builder.mCustomDrawableFactories)
        : null;
    mDrawDebugOverlay = builder.mDrawDebugOverlay;
  }

  @Nullable
  public ImmutableList<DrawableFactory> getCustomDrawableFactories() {
    return mCustomDrawableFactories;
  }

  public boolean shouldDrawDebugOverlay() {
    return mDrawDebugOverlay;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private List<DrawableFactory> mCustomDrawableFactories;
    private boolean mDrawDebugOverlay;

    /**
     * Add a custom drawable factory that will be used to create
     * Drawables for {@link com.facebook.imagepipeline.image.CloseableImage}s.
     *
     * @param factory the factory to use
     * @return the builder
     */
    public Builder addCustomDrawableFactory(DrawableFactory factory) {
      if (mCustomDrawableFactories == null) {
        mCustomDrawableFactories = new ArrayList<>();
      }
      mCustomDrawableFactories.add(factory);
      return this;
    }

    /**
     * Set whether a debug overlay that displays image information, like dimensions and size
     * should be drawn on top of a Drawee view.
     *
     * @param drawDebugOverlay true if the debug overlay should be drawn
     * @return the builder
     */
    public Builder setDrawDebugOverlay(boolean drawDebugOverlay) {
      mDrawDebugOverlay = drawDebugOverlay;
      return this;
    }

    public DraweeConfig build() {
      return new DraweeConfig(this);
    }
  }
}

/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.drawee.backends.pipeline;

import com.facebook.common.internal.ImmutableList;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.Supplier;
import com.facebook.common.internal.Suppliers;
import com.facebook.drawee.backends.pipeline.info.ImagePerfDataListener;
import com.facebook.imagepipeline.drawable.DrawableFactory;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Drawee configuration.
 */
public class DraweeConfig {

  @Nullable
  private final ImmutableList<DrawableFactory> mCustomDrawableFactories;
  @Nullable
  private final PipelineDraweeControllerFactory mPipelineDraweeControllerFactory;
  private final Supplier<Boolean> mDebugOverlayEnabledSupplier;
  @Nullable private final ImagePerfDataListener mImagePerfDataListener;

  private DraweeConfig(Builder builder) {
    mCustomDrawableFactories = builder.mCustomDrawableFactories != null
        ? ImmutableList.copyOf(builder.mCustomDrawableFactories)
        : null;
    mDebugOverlayEnabledSupplier = builder.mDebugOverlayEnabledSupplier != null
        ? builder.mDebugOverlayEnabledSupplier
        : Suppliers.of(false);
    mPipelineDraweeControllerFactory = builder.mPipelineDraweeControllerFactory;
    mImagePerfDataListener = builder.mImagePerfDataListener;
  }

  @Nullable
  public ImmutableList<DrawableFactory> getCustomDrawableFactories() {
    return mCustomDrawableFactories;
  }

  @Nullable
  public PipelineDraweeControllerFactory getPipelineDraweeControllerFactory() {
    return mPipelineDraweeControllerFactory;
  }

  @Nullable
  public ImagePerfDataListener getImagePerfDataListener() {
    return mImagePerfDataListener;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public Supplier<Boolean> getDebugOverlayEnabledSupplier() {
    return mDebugOverlayEnabledSupplier;
  }

  public static class Builder {

    private List<DrawableFactory> mCustomDrawableFactories;
    private Supplier<Boolean> mDebugOverlayEnabledSupplier;
    private PipelineDraweeControllerFactory mPipelineDraweeControllerFactory;
    private @Nullable ImagePerfDataListener mImagePerfDataListener;

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
     * @param drawDebugOverlay <code>true</code> if the debug overlay should be drawn
     * @return the builder
     */
    public Builder setDrawDebugOverlay(boolean drawDebugOverlay) {
      return setDebugOverlayEnabledSupplier(Suppliers.of(drawDebugOverlay));
    }

    /**
     * Set whether a debug overlay that displays image information, like dimensions and size
     * should be drawn on top of a Drawee view.
     *
     * @param debugOverlayEnabledSupplier should return <code>true</code> if the debug overlay
     * should be drawn
     * @return the builder
     */
    public Builder setDebugOverlayEnabledSupplier(Supplier<Boolean> debugOverlayEnabledSupplier) {
      Preconditions.checkNotNull(debugOverlayEnabledSupplier);
      mDebugOverlayEnabledSupplier = debugOverlayEnabledSupplier;
      return this;
    }

    /**
     * Set a PipelineDraweeControllerFactory to be used instead of the default one.
     *
     * @param factory the PipelineDraweeControllerFactory to use
     * @return the builder
     */
    public Builder setPipelineDraweeControllerFactory(PipelineDraweeControllerFactory factory) {
      mPipelineDraweeControllerFactory = factory;
      return this;
    }

    public Builder setImagePerfDataListener(@Nullable ImagePerfDataListener imagePerfDataListener) {
      mImagePerfDataListener = imagePerfDataListener;
      return this;
    }

    public DraweeConfig build() {
      return new DraweeConfig(this);
    }
  }
}

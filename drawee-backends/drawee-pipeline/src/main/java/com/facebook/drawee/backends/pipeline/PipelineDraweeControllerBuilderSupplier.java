/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.backends.pipeline;

import android.content.Context;
import com.facebook.common.executors.UiThreadImmediateExecutorService;
import com.facebook.common.internal.Supplier;
import com.facebook.drawee.backends.pipeline.info.ImagePerfDataListener;
import com.facebook.drawee.components.DeferredReleaser;
import com.facebook.drawee.controller.ControllerListener;
import com.facebook.fresco.ui.common.ControllerListener2;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.core.ImagePipelineFactory;
import java.util.Set;
import javax.annotation.Nullable;

public class PipelineDraweeControllerBuilderSupplier
    implements Supplier<PipelineDraweeControllerBuilder> {

  private final Context mContext;
  private final ImagePipeline mImagePipeline;
  private final PipelineDraweeControllerFactory mPipelineDraweeControllerFactory;
  private final Set<ControllerListener> mBoundControllerListeners;
  private final Set<ControllerListener2> mBoundControllerListeners2;
  private final @Nullable ImagePerfDataListener mDefaultImagePerfDataListener;

  public PipelineDraweeControllerBuilderSupplier(Context context) {
    this(context, null);
  }

  public PipelineDraweeControllerBuilderSupplier(
      Context context, @Nullable DraweeConfig draweeConfig) {
    this(context, ImagePipelineFactory.getInstance(), draweeConfig);
  }

  public PipelineDraweeControllerBuilderSupplier(
      Context context,
      ImagePipelineFactory imagePipelineFactory,
      @Nullable DraweeConfig draweeConfig) {
    this(context, imagePipelineFactory, null, null, draweeConfig);
  }

  public PipelineDraweeControllerBuilderSupplier(
      Context context,
      ImagePipelineFactory imagePipelineFactory,
      Set<ControllerListener> boundControllerListeners,
      Set<ControllerListener2> boundControllerListeners2,
      @Nullable DraweeConfig draweeConfig) {
    mContext = context;
    mImagePipeline = imagePipelineFactory.getImagePipeline();

    if (draweeConfig != null && draweeConfig.getPipelineDraweeControllerFactory() != null) {
      mPipelineDraweeControllerFactory = draweeConfig.getPipelineDraweeControllerFactory();
    } else {
      mPipelineDraweeControllerFactory = new PipelineDraweeControllerFactory();
    }
    mPipelineDraweeControllerFactory.init(
        context.getResources(),
        DeferredReleaser.getInstance(),
        imagePipelineFactory.getAnimatedDrawableFactory(context),
        UiThreadImmediateExecutorService.getInstance(),
        mImagePipeline.getBitmapMemoryCache(),
        draweeConfig != null ? draweeConfig.getCustomDrawableFactories() : null,
        draweeConfig != null ? draweeConfig.getDebugOverlayEnabledSupplier() : null);
    mBoundControllerListeners = boundControllerListeners;
    mBoundControllerListeners2 = boundControllerListeners2;

    mDefaultImagePerfDataListener =
        draweeConfig != null ? draweeConfig.getImagePerfDataListener() : null;
  }

  @Override
  public PipelineDraweeControllerBuilder get() {
    PipelineDraweeControllerBuilder pipelineDraweeControllerBuilder =
        new PipelineDraweeControllerBuilder(
            mContext,
            mPipelineDraweeControllerFactory,
            mImagePipeline,
            mBoundControllerListeners,
            mBoundControllerListeners2);
    return pipelineDraweeControllerBuilder.setPerfDataListener(mDefaultImagePerfDataListener);
  }
}

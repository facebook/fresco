/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl.source;

import androidx.annotation.Nullable;
import com.facebook.common.internal.Supplier;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.datasource.DataSources;
import com.facebook.fresco.vito.core.ImagePipelineUtils;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.infer.annotation.Nullsafe;

/** Empty image source if there is no image to be displayed. */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class EmptyImageSource implements VitoImageSource {

  public static final NullPointerException NO_REQUEST_EXCEPTION =
      new NullPointerException("No image request was specified!");

  private static final EmptyImageSource INSTANCE = new EmptyImageSource();

  protected static EmptyImageSource get() {
    return INSTANCE;
  }

  private EmptyImageSource() {}

  @Override
  @Nullable
  public ImageRequest maybeExtractFinalImageRequest(
      ImagePipelineUtils imagePipelineUtils, ImageOptions imageOptions) {
    return null;
  }

  @Override
  public Supplier<DataSource<CloseableReference<CloseableImage>>> createDataSourceSupplier(
      final ImagePipeline imagePipeline,
      final ImagePipelineUtils imagePipelineUtils,
      final ImageOptions imageOptions,
      final @Nullable Object callerContext,
      final @Nullable RequestListener requestListener,
      final String uiComponentId) {
    return new Supplier<DataSource<CloseableReference<CloseableImage>>>() {
      @Override
      public DataSource<CloseableReference<CloseableImage>> get() {
        return DataSources.immediateFailedDataSource(NO_REQUEST_EXCEPTION);
      }
    };
  }
}

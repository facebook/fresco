/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl.source;

import androidx.annotation.Nullable;
import com.facebook.common.internal.Objects;
import com.facebook.common.internal.Supplier;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.datasource.IncreasingQualityDataSourceSupplier;
import com.facebook.fresco.vito.core.ImagePipelineUtils;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.infer.annotation.Nullsafe;
import java.util.ArrayList;
import java.util.List;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class IncreasingQualityImageSource implements VitoImageSource {

  private final VitoImageSource mLowResImageSource;
  private final VitoImageSource mHighResImageSource;

  protected IncreasingQualityImageSource(
      VitoImageSource lowResImageSource, VitoImageSource highResImageSource) {
    mLowResImageSource = lowResImageSource;
    mHighResImageSource = highResImageSource;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) return false;
    IncreasingQualityImageSource other = (IncreasingQualityImageSource) obj;
    return mLowResImageSource.equals(other.mLowResImageSource)
        && mHighResImageSource.equals(other.mHighResImageSource);
  }

  @Override
  public int hashCode() {
    int result = mLowResImageSource.hashCode();
    result = 31 * result + mHighResImageSource.hashCode();
    return result;
  }

  public VitoImageSource getLowResImageSource() {
    return mLowResImageSource;
  }

  public VitoImageSource getHighResImageSource() {
    return mHighResImageSource;
  }

  @Override
  @Nullable
  public ImageRequest maybeExtractFinalImageRequest(
      ImagePipelineUtils imagePipelineUtils, ImageOptions imageOptions) {
    return mHighResImageSource.maybeExtractFinalImageRequest(imagePipelineUtils, imageOptions);
  }

  @Override
  public Supplier<DataSource<CloseableReference<CloseableImage>>> createDataSourceSupplier(
      final ImagePipeline imagePipeline,
      final ImagePipelineUtils imagePipelineUtils,
      final ImageOptions imageOptions,
      final @Nullable Object callerContext,
      final @Nullable RequestListener requestListener,
      final String uiComponentId) {
    final List<Supplier<DataSource<CloseableReference<CloseableImage>>>> suppliers =
        new ArrayList<>(2);
    suppliers.add(
        mHighResImageSource.createDataSourceSupplier(
            imagePipeline,
            imagePipelineUtils,
            imageOptions,
            callerContext,
            requestListener,
            uiComponentId));
    suppliers.add(
        mLowResImageSource.createDataSourceSupplier(
            imagePipeline,
            imagePipelineUtils,
            imageOptions,
            callerContext,
            requestListener,
            uiComponentId));
    return IncreasingQualityDataSourceSupplier.create(suppliers);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("lowRes", mLowResImageSource)
        .add("highRes", mHighResImageSource)
        .toString();
  }
}

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
import com.facebook.datasource.FirstAvailableDataSourceSupplier;
import com.facebook.fresco.vito.core.ImagePipelineUtils;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.source.ImageSource;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.infer.annotation.Nullsafe;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class FirstAvailableImageSource implements VitoImageSource {

  private final ImageSource[] mFirstAvailableImageSources;

  protected FirstAvailableImageSource(final ImageSource... firstAvailableImageSources) {
    mFirstAvailableImageSources = firstAvailableImageSources;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) return false;
    FirstAvailableImageSource other = (FirstAvailableImageSource) obj;
    return Arrays.equals(mFirstAvailableImageSources, other.mFirstAvailableImageSources);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(mFirstAvailableImageSources);
  }

  public ImageSource[] getSources() {
    return mFirstAvailableImageSources;
  }

  @Override
  @Nullable
  public ImageRequest maybeExtractFinalImageRequest(
      ImagePipelineUtils imagePipelineUtils, ImageOptions imageOptions) {
    for (ImageSource imageSource : mFirstAvailableImageSources) {
      if (imageSource instanceof VitoImageSource) {
        ImageRequest imageRequest =
            ((VitoImageSource) imageSource)
                .maybeExtractFinalImageRequest(imagePipelineUtils, imageOptions);
        if (imageRequest != null) {
          return imageRequest;
        }
      }
    }
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
    final List<Supplier<DataSource<CloseableReference<CloseableImage>>>> suppliers =
        new ArrayList<>(mFirstAvailableImageSources.length);
    for (ImageSource source : mFirstAvailableImageSources) {
      if (source instanceof VitoImageSource) {
        suppliers.add(
            ((VitoImageSource) source)
                .createDataSourceSupplier(
                    imagePipeline,
                    imagePipelineUtils,
                    imageOptions,
                    callerContext,
                    requestListener,
                    uiComponentId));
      } else {
        throw new IllegalArgumentException(
            "FirstAvailableImageSource must be VitoImageSource: " + source);
      }
    }
    return FirstAvailableDataSourceSupplier.create(suppliers);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this).addValue(mFirstAvailableImageSources).toString();
  }
}

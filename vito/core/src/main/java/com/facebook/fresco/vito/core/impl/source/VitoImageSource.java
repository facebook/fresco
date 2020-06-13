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
import com.facebook.fresco.vito.core.ImagePipelineUtils;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.source.ImageSource;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.imagepipeline.request.ImageRequest;

public interface VitoImageSource extends ImageSource {

  /**
   * Get the final image request for the last image if known. In some cases, like for @{link
   * FirstAvailableImageSource}, we do not know the final image request, so this method will return
   * null.
   *
   * @param imagePipelineUtils util class to create the final image request
   * @param imageOptions the image options to use, important if for example rounding is done at
   *     decode time
   * @return the final image request or null if not possible to determine
   */
  @Nullable
  ImageRequest maybeExtractFinalImageRequest(
      ImagePipelineUtils imagePipelineUtils, ImageOptions imageOptions);

  Supplier<DataSource<CloseableReference<CloseableImage>>> createDataSourceSupplier(
      final ImagePipeline imagePipeline,
      final ImagePipelineUtils imagePipelineUtils,
      final ImageOptions imageOptions,
      final @Nullable Object callerContext,
      final @Nullable RequestListener requestListener,
      final String uiComponentId);
}

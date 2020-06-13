/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core;

import android.content.res.Resources;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.source.ImageSource;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.listener.RequestListener;
import javax.annotation.Nullable;

public interface VitoImagePipeline {

  VitoImageRequest createImageRequest(
      Resources resources, ImageSource imageSource, @Nullable ImageOptions options);

  @Nullable
  CloseableReference<CloseableImage> getCachedImage(VitoImageRequest imageRequest);

  DataSource<CloseableReference<CloseableImage>> fetchDecodedImage(
      VitoImageRequest imageSource,
      @Nullable Object callerContext,
      @Nullable RequestListener requestListener,
      @Nullable long uiComponentId);
}

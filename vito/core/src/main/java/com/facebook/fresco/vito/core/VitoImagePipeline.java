/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core;

import android.content.res.Resources;
import android.net.Uri;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.imagepipeline.multiuri.MultiUri;
import javax.annotation.Nullable;

public interface VitoImagePipeline {

  VitoImageRequest createImageRequest(
      Resources resources,
      @Nullable Uri uri,
      @Nullable MultiUri multiUri,
      @Nullable ImageOptions options,
      @Nullable Object callerContext);

  @Nullable
  CloseableReference<CloseableImage> getCachedImage(VitoImageRequest imageRequest);

  DataSource<CloseableReference<CloseableImage>> fetchDecodedImage(
      VitoImageRequest imageRequest,
      @Nullable Object callerContext,
      @Nullable RequestListener requestListener,
      @Nullable long uiComponentId);
}

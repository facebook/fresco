/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl.source;

import android.net.Uri;
import com.facebook.fresco.vito.source.ImageSource;
import com.facebook.fresco.vito.source.ImageSourceProvider;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.facebook.infer.annotation.Nullsafe;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class ImageSourceProviderImpl implements ImageSourceProvider.Implementation {

  @Override
  public VitoImageSource emptySource() {
    return EmptyImageSource.get();
  }

  @Override
  public ImageSource singleImageRequest(ImageRequest imageRequest) {
    return new SingleImageSource(imageRequest);
  }

  @Override
  public ImageSource singleImageRequest(
      ImageRequest imageRequest, ImageRequest.RequestLevel requestLevelForFetch) {
    return new SingleImageSource(imageRequest, requestLevelForFetch);
  }

  @Override
  public ImageSource singleUri(Uri uri) {
    return singleImageRequest(ImageRequestBuilder.newBuilderWithSource(uri).build());
  }

  @Override
  public ImageSource firstAvailable(ImageSource... imageSources) {
    return new FirstAvailableImageSource(imageSources);
  }

  @Override
  public ImageSource increasingQuality(
      ImageSource lowResImageSource, ImageSource highResImageSource) {
    return new IncreasingQualityImageSource(
        (VitoImageSource) lowResImageSource, (VitoImageSource) highResImageSource);
  }
}

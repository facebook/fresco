/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core;

import android.net.Uri;
import com.facebook.fresco.vito.options.DecodedImageOptions;
import com.facebook.fresco.vito.options.EncodedImageOptions;
import com.facebook.imagepipeline.request.ImageRequest;
import javax.annotation.Nullable;

public interface ImagePipelineUtils {

  @Nullable
  ImageRequest buildImageRequest(@Nullable Uri uri, DecodedImageOptions imageOptions);

  @Nullable
  ImageRequest wrapDecodedImageRequest(
      ImageRequest originalRequest, DecodedImageOptions imageOptions);

  @Nullable
  ImageRequest buildEncodedImageRequest(@Nullable Uri uri, EncodedImageOptions imageOptions);
}

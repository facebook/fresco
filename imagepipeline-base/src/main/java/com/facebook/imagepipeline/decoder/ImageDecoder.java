/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.decoder;

import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.image.QualityInfo;

/**
 * Image decoder interface. Takes an {@link EncodedImage} and creates a {@link CloseableImage}.
 */
public interface ImageDecoder {

  CloseableImage decode(
      EncodedImage encodedImage,
      int length,
      QualityInfo qualityInfo,
      ImageDecodeOptions options);
}

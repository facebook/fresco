/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
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

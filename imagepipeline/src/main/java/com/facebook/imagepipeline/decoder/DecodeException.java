/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.decoder;

import com.facebook.imagepipeline.image.EncodedImage;

public class DecodeException extends RuntimeException {

  private final EncodedImage mEncodedImage;

  public DecodeException(String message, EncodedImage encodedImage) {
    super(message);
    mEncodedImage = encodedImage;
  }

  public DecodeException(String message, Throwable t, EncodedImage encodedImage) {
    super(message, t);
    mEncodedImage = encodedImage;
  }

  public EncodedImage getEncodedImage() {
    return mEncodedImage;
  }
}

/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
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

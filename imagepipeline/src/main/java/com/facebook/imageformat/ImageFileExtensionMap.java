/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.imageformat;

public class ImageFileExtensionMap {

  private ImageFileExtensionMap() {

  }

  /**
   * Maps an image format to the file extension
   * @param imageFormat image format
   * @return  file extension for the image format
   * @throws UnsupportedOperationException
   */
  public static String getFileExtension(ImageFormat imageFormat)
      throws UnsupportedOperationException {

    switch (imageFormat) {
      case WEBP_SIMPLE:
      case WEBP_LOSSLESS:
      case WEBP_EXTENDED:
      case WEBP_EXTENDED_WITH_ALPHA:
      case WEBP_ANIMATED:
        return "webp";
      case JPEG:
        return "jpeg";
      case PNG:
        return "png";
      case GIF:
        return "gif";
      case BMP:
        return "bmp";
      default:
        throw new UnsupportedOperationException("Unknown image format " + imageFormat.name());
    }
  }
}

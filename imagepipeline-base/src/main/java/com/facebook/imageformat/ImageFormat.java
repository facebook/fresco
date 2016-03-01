/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imageformat;

/**
 * Enum representing all used image formats.
 */
public enum ImageFormat {

  WEBP_SIMPLE,
  WEBP_LOSSLESS,
  WEBP_EXTENDED,
  WEBP_EXTENDED_WITH_ALPHA,
  WEBP_ANIMATED,
  JPEG,
  PNG,
  GIF,
  BMP,
  /**
   * Unknown image. This is needed in case we fail to detect any type for particular image.
   */
  UNKNOWN;

  public static boolean isWebpFormat(ImageFormat imageFormat) {
    return imageFormat == WEBP_SIMPLE ||
        imageFormat == WEBP_LOSSLESS ||
        imageFormat == WEBP_EXTENDED ||
        imageFormat == WEBP_EXTENDED_WITH_ALPHA ||
        imageFormat == WEBP_ANIMATED;
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

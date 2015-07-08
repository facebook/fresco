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
}

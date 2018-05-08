/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.drawee.backends.pipeline.info;

public class ImagePerfUtils {

  public static String toString(@ImageLoadStatus int imageLoadStatus) {
    switch (imageLoadStatus) {
      case ImageLoadStatus.REQUESTED:
        return "requested";
      case ImageLoadStatus.ORIGIN_AVAILABLE:
        return "origin_available";
      case ImageLoadStatus.SUCCESS:
        return "success";
      case ImageLoadStatus.CANCELED:
        return "canceled";
      case ImageLoadStatus.ERROR:
        return "error";
      default:
        return "unknown";
    }
  }

  private ImagePerfUtils() {}
}

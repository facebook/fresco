/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.comparison.urlsfetcher;

/** Formats of images we download from imgur. */
public enum ImageFormat {
  JPEG("image/jpeg"),
  PNG("image/png"),
  GIF("image/gif");

  private static final ImageFormat[] VALUES = values();

  public final String mime;

  private ImageFormat(final String mime) {
    this.mime = mime;
  }

  public static ImageFormat getImageFormatForMime(String mime) {
    for (ImageFormat type : VALUES) {
      if (type.mime.equals(mime)) {
        return type;
      }
    }
    return null;
  }
}

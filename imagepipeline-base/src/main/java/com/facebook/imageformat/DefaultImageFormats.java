/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.imageformat;

import java.util.ArrayList;
import java.util.List;

import com.facebook.common.internal.ImmutableList;

/**
 * Default image formats that Fresco supports.
 */
public final class DefaultImageFormats {

  public static final ImageFormat JPEG = new ImageFormat("JPEG", "jpeg");
  public static final ImageFormat PNG = new ImageFormat("PNG", "png");
  public static final ImageFormat GIF = new ImageFormat("GIF", "gif");
  public static final ImageFormat BMP = new ImageFormat("BMP", "bmp");
  public static final ImageFormat WEBP_SIMPLE = new ImageFormat("WEBP_SIMPLE", "webp");
  public static final ImageFormat WEBP_LOSSLESS = new ImageFormat("WEBP_LOSSLESS", "webp");
  public static final ImageFormat WEBP_EXTENDED = new ImageFormat("WEBP_EXTENDED", "webp");
  public static final ImageFormat WEBP_EXTENDED_WITH_ALPHA =
      new ImageFormat("WEBP_EXTENDED_WITH_ALPHA", "webp");
  public static final ImageFormat WEBP_ANIMATED = new ImageFormat("WEBP_ANIMATED", "webp");

  private static ImmutableList<ImageFormat> sAllDefaultFormats;

  /**
   * Check if the given image format is a WebP image format (static or animated).
   *
   * @param imageFormat the image format to check
   * @return true if WebP format
   */
  public static boolean isWebpFormat(ImageFormat imageFormat) {
    return isStaticWebpFormat(imageFormat) ||
        imageFormat == WEBP_ANIMATED;
  }

  /**
   * Check if the given image format is static WebP (not animated).
   *
   * @param imageFormat the image format to check
   * @return true if static WebP
   */
  public static boolean isStaticWebpFormat(ImageFormat imageFormat) {
    return imageFormat == WEBP_SIMPLE ||
        imageFormat == WEBP_LOSSLESS ||
        imageFormat == WEBP_EXTENDED ||
        imageFormat == WEBP_EXTENDED_WITH_ALPHA;
  }

  /**
   * Get all default formats supported by Fresco.
   * Does not include {@link ImageFormat#UNKNOWN}.
   *
   * @return all supported default formats
   */
  public static List<ImageFormat> getDefaultFormats() {
    if (sAllDefaultFormats == null) {
      List<ImageFormat> mDefaultFormats = new ArrayList<>(9);
      mDefaultFormats.add(JPEG);
      mDefaultFormats.add(PNG);
      mDefaultFormats.add(GIF);
      mDefaultFormats.add(BMP);
      mDefaultFormats.add(WEBP_SIMPLE);
      mDefaultFormats.add(WEBP_LOSSLESS);
      mDefaultFormats.add(WEBP_EXTENDED);
      mDefaultFormats.add(WEBP_EXTENDED_WITH_ALPHA);
      mDefaultFormats.add(WEBP_ANIMATED);
      sAllDefaultFormats = ImmutableList.copyOf(mDefaultFormats);
    }
    return sAllDefaultFormats;
  }

  private DefaultImageFormats() {
  }
}

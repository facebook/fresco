/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.nativecode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Base64;

import com.facebook.common.internal.DoNotStrip;
import com.facebook.common.internal.Preconditions;
import com.facebook.imageformat.ImageFormat;

/**
 * Helper methods for modifying webp static images.
 */
@DoNotStrip
public class WebpTranscoder {

  static {
    ImagePipelineNativeLoader.load();
  }

  /**
   * BASE64 encoded extended WebP image.
   */
  private static final String VP8X_WEBP_BASE64 = "UklGRkoAAABXRUJQVlA4WAoAAAAQAAAAAAAAAAAAQUxQSAw" +
      "AAAARBxAR/Q9ERP8DAABWUDggGAAAABQBAJ0BKgEAAQAAAP4AAA3AAP7mtQAAAA==";
  private static final boolean mIsExtendedWebpSupported = isExtendedWebpSupported();

  /**
   * Checks whether underlying platform supports extended WebPs
   */
  private static boolean isExtendedWebpSupported() {
    // Lossless and extended formats are supported on Android 4.2.1+
    // Unfortunately SDK_INT is not enough to distinguish 4.2 and 4.2.1
    // (both are API level 17 (JELLY_BEAN_MR1))
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
      return false;
    }

    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR1) {
      // Let's test if extended webp is supported
      // To this end we will try to decode bounds of vp8x webp with alpha channel
      byte[] decodedBytes = Base64.decode(VP8X_WEBP_BASE64, Base64.DEFAULT);
      BitmapFactory.Options opts = new BitmapFactory.Options();
      opts.inJustDecodeBounds = true;
      BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length, opts);

      // If Android managed to find appropriate decoder then opts.outHeight and opts.outWidth
      // should be set. Warning ! Unfortunately we can not assume that outMimeType is set.
      // Android guys forgot to update logic for mime types when they introduced support for webp.
      // For example, on 4.2.2 this field is not set for webp images.
      if (opts.outHeight != 1 || opts.outWidth != 1) {
        return false;
      }
    }

    return true;
  }

  /**
   * @return true if given type of WebP is supported natively by the framework
   */
  public static boolean isWebpNativelySupported(ImageFormat webpFormat) {
    switch (webpFormat) {
      case WEBP_SIMPLE: // Simple WebPs are supported on Android 4.0+
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
      case WEBP_LOSSLESS:
      case WEBP_EXTENDED:
      case WEBP_EXTENDED_WITH_ALPHA:
        return mIsExtendedWebpSupported;
      case WEBP_ANIMATED:
        return false;
      default:
        Preconditions.checkArgument(false);
        return false;
    }
  }

  /**
   * Transcodes webp image given by input stream into jpeg.
   */
  public static void transcodeWebpToJpeg(
      InputStream inputStream,
      OutputStream outputStream,
      int quality) throws IOException {
    nativeTranscodeWebpToJpeg(
        Preconditions.checkNotNull(inputStream),
        Preconditions.checkNotNull(outputStream),
        quality);
  }

  /**
   * Transcodes Webp image given by input stream into png.
   */
  public static void transcodeWebpToPng(
      InputStream inputStream,
      OutputStream outputStream) throws IOException {
    nativeTranscodeWebpToPng(
        Preconditions.checkNotNull(inputStream),
        Preconditions.checkNotNull(outputStream));
  }

  @DoNotStrip
  private static native void nativeTranscodeWebpToJpeg(
      InputStream inputStream,
      OutputStream outputStream,
      int quality) throws IOException;

  @DoNotStrip
  private static native void nativeTranscodeWebpToPng(
      InputStream inputStream,
      OutputStream outputStream) throws IOException;
}

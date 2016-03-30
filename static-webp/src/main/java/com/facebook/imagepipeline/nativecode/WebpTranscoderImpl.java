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
import com.facebook.common.webp.WebpSupportStatus;
import com.facebook.common.soloader.SoLoaderShim;

/**
 * Helper methods for modifying webp static images.
 */
@DoNotStrip
public class WebpTranscoderImpl implements WebpTranscoder {

  static {
    SoLoaderShim.loadLibrary("static-webp");
  }

  /**
   * @return true if given type of WebP is supported natively by the framework
   */
  @Override
  public boolean isWebpNativelySupported(ImageFormat webpFormat) {
    switch (webpFormat) {
      case WEBP_SIMPLE: // Simple WebPs are supported on Android 4.0+
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
      case WEBP_LOSSLESS:
      case WEBP_EXTENDED:
      case WEBP_EXTENDED_WITH_ALPHA:
        return WebpSupportStatus.sIsExtendedWebpSupported;
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
  @Override
  public void transcodeWebpToJpeg(
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
  @Override
  public void transcodeWebpToPng(
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

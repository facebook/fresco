/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.nativecode;

import android.os.Build;
import com.facebook.common.internal.DoNotStrip;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.webp.WebpSupportStatus;
import com.facebook.imageformat.DefaultImageFormats;
import com.facebook.imageformat.ImageFormat;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Helper methods for modifying webp static images.
 */
@DoNotStrip
public class WebpTranscoderImpl implements WebpTranscoder {

  /**
   * @return true if given type of WebP is supported natively by the framework
   */
  @Override
  public boolean isWebpNativelySupported(ImageFormat webpFormat) {
    if (webpFormat == DefaultImageFormats.WEBP_SIMPLE) {
      // Simple WebPs are supported on Android 4.0+
      return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
    } else if (webpFormat == DefaultImageFormats.WEBP_LOSSLESS ||
        webpFormat == DefaultImageFormats.WEBP_EXTENDED ||
        webpFormat == DefaultImageFormats.WEBP_EXTENDED_WITH_ALPHA) {
      return WebpSupportStatus.sIsExtendedWebpSupported;
    } else if (webpFormat == DefaultImageFormats.WEBP_ANIMATED) {
      return false;
    }
    throw new IllegalArgumentException("Image format is not a WebP.");
  }

  /**
   * Transcodes webp image given by input stream into jpeg.
   */
  @Override
  public void transcodeWebpToJpeg(
      InputStream inputStream,
      OutputStream outputStream,
      int quality) throws IOException {
    StaticWebpNativeLoader.ensure();
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
    StaticWebpNativeLoader.ensure();
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

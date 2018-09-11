/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.nativecode;

import com.facebook.imageformat.ImageFormat;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * The abstraction for WebpTranscoder
 */
public interface WebpTranscoder {

  /**
  * @return true if given type of WebP is supported natively by the framework
  */
  boolean isWebpNativelySupported(ImageFormat webpFormat);

  /**
   * Transcodes webp image given by input stream into jpeg.
   */
  void transcodeWebpToJpeg(
      InputStream inputStream,
      OutputStream outputStream,
      int quality) throws IOException;

  /**
   * Transcodes Webp image given by input stream into png.
   */
  void transcodeWebpToPng(
      InputStream inputStream,
      OutputStream outputStream) throws IOException;

}

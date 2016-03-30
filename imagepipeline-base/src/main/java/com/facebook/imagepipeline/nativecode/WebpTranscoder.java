/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.nativecode;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import com.facebook.imageformat.ImageFormat;

/**
 * The abstraction for WebpTranscoder
 */
public interface WebpTranscoder {

  /**
  * @return true if given type of WebP is supported natively by the framework
  */
  public boolean isWebpNativelySupported(ImageFormat webpFormat);

  /**
   * Transcodes webp image given by input stream into jpeg.
   */
  public void transcodeWebpToJpeg(
      InputStream inputStream,
      OutputStream outputStream,
      int quality) throws IOException;

  /**
   * Transcodes Webp image given by input stream into png.
   */
  public void transcodeWebpToPng(
      InputStream inputStream,
      OutputStream outputStream) throws IOException;

}

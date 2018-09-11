/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.nativecode;

/**
 * This is the class responsible to return the WebpTranscoder if any
 */
public class WebpTranscoderFactory {

  private static WebpTranscoder sWebpTranscoder;

  public static boolean sWebpTranscoderPresent = false;

  static {
    try {
      sWebpTranscoder = (WebpTranscoder) Class
          .forName("com.facebook.imagepipeline.nativecode.WebpTranscoderImpl")
          .newInstance();
      sWebpTranscoderPresent = true;
    } catch (Throwable e) {
      sWebpTranscoderPresent = false;
    }
  }

  public static WebpTranscoder getWebpTranscoder() {
    return sWebpTranscoder;
  }

}

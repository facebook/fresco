/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.nativecode;

import static com.facebook.common.webp.WebpSupportStatus.sWebpLibraryPresent;

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

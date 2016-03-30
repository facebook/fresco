/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.common.webp;

import java.io.FileDescriptor;
import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;

/**
 * Interface for a bitmap factory that can decode WebP images even on versions of Android that
 * don't support it.
 * <p> Implementation is found in the optional static-webp library. To use, add the following to
 * your build.gradle file: <code>compile 'com.facebook.fresco:static-webp:${FRESCO_VERSION}'</code>
 */
public interface WebpBitmapFactory {

  Bitmap decodeFileDescriptor(
      FileDescriptor fd,
      Rect outPadding,
      BitmapFactory.Options opts);

  Bitmap decodeStream(
      InputStream inputStream,
      Rect outPadding,
      BitmapFactory.Options opts);

  Bitmap decodeFile(
      String pathName,
      BitmapFactory.Options opts);

  Bitmap decodeByteArray(
      byte[] array,
      int offset,
      int length,
      BitmapFactory.Options opts);

}

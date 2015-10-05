/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.webpsupport;

import java.io.FileDescriptor;
import java.io.InputStream;

import android.graphics.BitmapFactory;
import android.graphics.Rect;
import com.facebook.dalvikdistract.DalvikDistract;
import com.facebook.proguard.annotations.DoNotStrip;

import static com.facebook.imagepipeline.webp.WebpSupportStatus.sIsWebpSupportRequired;

@DoNotStrip
public class WebpBitmapFactoryHack {

  public static void addDalvikHooks() {
    if (sIsWebpSupportRequired) {
      try {
        new DalvikDistract()
            .hook(
                BitmapFactory.class.getDeclaredMethod(
                    "decodeByteArray",
                    byte[].class,
                    int.class,
                    int.class,
                    BitmapFactory.Options.class),
                WebpBitmapFactory.class.getDeclaredMethod(
                    "hookDecodeByteArray",
                    byte[].class,
                    int.class,
                    int.class,
                    BitmapFactory.Options.class),
                WebpBitmapFactory.class.getDeclaredMethod(
                    "originalDecodeByteArray",
                    byte[].class,
                    int.class,
                    int.class,
                    BitmapFactory.Options.class))

            .hook(
                BitmapFactory.class.getDeclaredMethod(
                    "decodeStream",
                    InputStream.class,
                    Rect.class,
                    BitmapFactory.Options.class),
                WebpBitmapFactory.class.getDeclaredMethod(
                    "hookDecodeStream",
                    InputStream.class,
                    Rect.class,
                    BitmapFactory.Options.class),
                WebpBitmapFactory.class.getDeclaredMethod(
                    "originalDecodeStream",
                    InputStream.class,
                    Rect.class,
                    BitmapFactory.Options.class))

            .hook(
                BitmapFactory.class.getDeclaredMethod(
                    "decodeFile",
                    String.class,
                    BitmapFactory.Options.class),
                WebpBitmapFactory.class.getDeclaredMethod(
                    "hookDecodeFile",
                    String.class,
                    BitmapFactory.Options.class),
                WebpBitmapFactory.class.getDeclaredMethod(
                    "originalDecodeFile",
                    String.class,
                    BitmapFactory.Options.class))

            .hook(
                BitmapFactory.class.getDeclaredMethod(
                    "decodeFileDescriptor",
                    FileDescriptor.class,
                    Rect.class,
                    BitmapFactory.Options.class),
                WebpBitmapFactory.class.getDeclaredMethod(
                    "hookDecodeFileDescriptor",
                    FileDescriptor.class,
                    Rect.class,
                    BitmapFactory.Options.class),
                WebpBitmapFactory.class.getDeclaredMethod(
                    "originalDecodeFileDescriptor",
                    FileDescriptor.class,
                    Rect.class,
                    BitmapFactory.Options.class))

            .commit();
      } catch (NoSuchMethodException exp) {
        // Shouldn't happen
      }
    }
  }

  static void cleanDalvikHooks() {
    if (sIsWebpSupportRequired) {
      try {
        new DalvikDistract()
            .unhook(
                BitmapFactory.class.getDeclaredMethod(
                    "decodeByteArray",
                    byte[].class,
                    int.class,
                    int.class,
                    BitmapFactory.Options.class))

            .unhook(
                BitmapFactory.class.getDeclaredMethod(
                    "decodeStream",
                    InputStream.class,
                    Rect.class,
                    BitmapFactory.Options.class))

            .unhook(
                BitmapFactory.class.getDeclaredMethod(
                    "decodeFile",
                    String.class,
                    BitmapFactory.Options.class))

            .unhook(
                BitmapFactory.class.getDeclaredMethod(
                    "decodeFileDescriptor",
                    FileDescriptor.class,
                    Rect.class,
                    BitmapFactory.Options.class))

            .commit();
      } catch (NoSuchMethodException exp) {
        // Shouldn't happen
      }
    }
  }

}

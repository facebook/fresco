/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.nativecode;

import static com.facebook.soloader.nativeloader.NativeLoaderDelegate.SKIP_MERGED_JNI_ONLOAD;

import android.os.Build;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.soloader.nativeloader.NativeLoader;

/** Single place responsible for ensuring that native-imagetranscoder.so is loaded */
@Nullsafe(Nullsafe.Mode.STRICT)
public class NativeJpegTranscoderSoLoader {
  private static boolean sInitialized;

  public static synchronized void ensure() {
    if (!sInitialized) {
      // On Android 4.1.2 the loading of the native-imagetranscoder native library can fail because
      // of the dependency with fb_jpegturbo. In this case we have to explicitely load that
      // library
      if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
        try {
          NativeLoader.loadLibrary("fb_jpegturbo", SKIP_MERGED_JNI_ONLOAD);
        } catch (UnsatisfiedLinkError error) {
          // Head in the sand
        }
      }
      NativeLoader.loadLibrary("native-imagetranscoder");
      sInitialized = true;
    }
  }
}

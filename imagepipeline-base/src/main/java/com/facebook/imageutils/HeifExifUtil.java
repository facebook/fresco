/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imageutils;

import android.media.ExifInterface;
import android.os.Build;
import androidx.annotation.RequiresApi;
import com.facebook.common.logging.FLog;
import com.facebook.soloader.DoNotOptimize;
import java.io.IOException;
import java.io.InputStream;

/**
 * Specialization of EXIF utilities for HEIF files. The specialization allows us to restrict
 * ourselves to Android N where {@link ExifInterface} can wrap around an input stream. Actual
 * support for HEIF was only added in Android P.
 */
public class HeifExifUtil {

  public static final String TAG = "HeifExifUtil";

  public static int getOrientation(final InputStream inputStream) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      return HeifExifUtilAndroidN.getOrientation(inputStream);
    } else {
      FLog.d(TAG, "Trying to read Heif Exif information before Android N -> ignoring");
      return ExifInterface.ORIENTATION_UNDEFINED;
    }
  }

  @DoNotOptimize
  private static class HeifExifUtilAndroidN {

    @RequiresApi(api = Build.VERSION_CODES.N)
    static int getOrientation(final InputStream inputStream) {
      try {
        final ExifInterface exifInterface = new ExifInterface(inputStream);
        return exifInterface.getAttributeInt(
            ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
      } catch (final IOException e) {
        FLog.d(TAG, "Failed reading Heif Exif orientation -> ignoring", e);
        return ExifInterface.ORIENTATION_UNDEFINED;
      }
    }
  }
}

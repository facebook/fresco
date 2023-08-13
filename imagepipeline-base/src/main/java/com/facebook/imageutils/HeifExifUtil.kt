/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imageutils

import android.media.ExifInterface
import android.os.Build
import androidx.annotation.RequiresApi
import com.facebook.common.logging.FLog
import com.facebook.imageutils.HeifExifUtil.HeifExifUtilAndroidN
import com.facebook.soloader.DoNotOptimize
import java.io.IOException
import java.io.InputStream

/**
 * Specialization of EXIF utilities for HEIF files. The specialization allows us to restrict
 * ourselves to Android N where [ExifInterface] can wrap around an input stream. Actual support for
 * HEIF was only added in Android P.
 */
object HeifExifUtil {

  const val TAG = "HeifExifUtil"

  @JvmStatic
  fun getOrientation(inputStream: InputStream?): Int {
    return if (inputStream == null) {
      FLog.d(TAG, "Trying to read Heif Exif from null inputStream -> ignoring")
      ExifInterface.ORIENTATION_UNDEFINED
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      HeifExifUtilAndroidN.getOrientation(inputStream)
    } else {
      FLog.d(TAG, "Trying to read Heif Exif information before Android N -> ignoring")
      ExifInterface.ORIENTATION_UNDEFINED
    }
  }

  @DoNotOptimize
  private object HeifExifUtilAndroidN {
    @JvmStatic
    @RequiresApi(api = Build.VERSION_CODES.N)
    fun getOrientation(inputStream: InputStream): Int =
        try {
          val exifInterface = ExifInterface(inputStream)
          exifInterface.getAttributeInt(
              ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } catch (e: IOException) {
          FLog.d(TAG, "Failed reading Heif Exif orientation -> ignoring", e)
          ExifInterface.ORIENTATION_UNDEFINED
        }
  }
}

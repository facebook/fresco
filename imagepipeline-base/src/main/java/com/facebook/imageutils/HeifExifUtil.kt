/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imageutils

import androidx.exifinterface.media.ExifInterface
import com.facebook.common.logging.FLog
import java.io.IOException
import java.io.InputStream

/**
 * Specialization of EXIF utilities for HEIF files. Actual support for HEIF was only added in
 * Android P.
 */
object HeifExifUtil {

  private const val TAG: String = "HeifExifUtil"

  @JvmStatic
  fun getOrientation(inputStream: InputStream?): Int {
    if (inputStream == null) {
      FLog.d(TAG, "Trying to read Heif Exif from null inputStream -> ignoring")
      return ExifInterface.ORIENTATION_UNDEFINED
    }

    return try {
      val exifInterface = ExifInterface(inputStream)
      exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    } catch (e: IOException) {
      FLog.d(TAG, "Failed reading Heif Exif orientation -> ignoring", e)
      ExifInterface.ORIENTATION_UNDEFINED
    }
  }
}

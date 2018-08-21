/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.imagepipeline.transcoder;

import android.media.ExifInterface;

public class JpegTranscoderUtils {

  public static final int MIN_QUALITY = 0;
  public static final int MAX_QUALITY = 100;
  public static final int MIN_SCALE_NUMERATOR = 1;
  public static final int MAX_SCALE_NUMERATOR = 16;
  public static final int SCALE_DENOMINATOR = 8;

  /**
   * @return true if and only if given number of degrees is allowed rotation angle, that is it is
   *     equal to 0, 90, 180 or 270
   */
  public static boolean isRotationAngleAllowed(int degrees) {
    return (degrees >= 0) && (degrees <= 270) && (degrees % 90 == 0);
  }

  /** @return true if and only if given value is a valid EXIF orientation */
  public static boolean isExifOrientationAllowed(int exifOrientation) {
    switch (exifOrientation) {
      case ExifInterface.ORIENTATION_NORMAL:
      case ExifInterface.ORIENTATION_ROTATE_90:
      case ExifInterface.ORIENTATION_ROTATE_180:
      case ExifInterface.ORIENTATION_ROTATE_270:
      case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
      case ExifInterface.ORIENTATION_FLIP_VERTICAL:
      case ExifInterface.ORIENTATION_TRANSPOSE:
      case ExifInterface.ORIENTATION_TRANSVERSE:
        return true;
      default:
        return false;
    }
  }
}

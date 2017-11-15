/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.nativecode;

import android.media.ExifInterface;
import com.facebook.common.internal.DoNotStrip;
import com.facebook.common.internal.Preconditions;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Helper methods for modifying jpeg images.
 */
@DoNotStrip
public class JpegTranscoder {

  static {
    ImagePipelineNativeLoader.load();
  }

  public static final int MIN_QUALITY = 0;
  public static final int MAX_QUALITY = 100;
  public static final int MIN_SCALE_NUMERATOR = 1;
  public static final int MAX_SCALE_NUMERATOR = 16;
  public static final int SCALE_DENOMINATOR = 8;

  /**
   * @return true if and only if given number of degrees is allowed rotation angle, that is
   *   it is equal to 0, 90, 180 or 270
   */
  public static boolean isRotationAngleAllowed(int degrees) {
    return (degrees >= 0) && (degrees <= 270) && (degrees % 90 == 0);
  }

  /**
   * @return true if and only if given value is a valid EXIF orientation
   */
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

  /**
   * Downscales and rotates jpeg image
   *
   * @param inputStream
   * @param outputStream
   * @param rotationAngle 0, 90, 180 or 270
   * @param scaleNumerator 1 - 16, image will be scaled using scaleNumerator/8 factor
   * @param quality 1 - 100
   */
  public static void transcodeJpeg(
      final InputStream inputStream,
      final OutputStream outputStream,
      final int rotationAngle,
      final int scaleNumerator,
      final int quality)
      throws IOException {
    Preconditions.checkArgument(scaleNumerator >= MIN_SCALE_NUMERATOR);
    Preconditions.checkArgument(scaleNumerator <= MAX_SCALE_NUMERATOR);
    Preconditions.checkArgument(quality >= MIN_QUALITY);
    Preconditions.checkArgument(quality <= MAX_QUALITY);
    Preconditions.checkArgument(isRotationAngleAllowed(rotationAngle));
    Preconditions.checkArgument(
        scaleNumerator != SCALE_DENOMINATOR || rotationAngle != 0,
        "no transformation requested");
    nativeTranscodeJpeg(
        Preconditions.checkNotNull(inputStream),
        Preconditions.checkNotNull(outputStream),
        rotationAngle,
        scaleNumerator,
        quality);
  }

  @DoNotStrip
  private static native void nativeTranscodeJpeg(
      InputStream inputStream,
      OutputStream outputStream,
      int rotationAngle,
      int scaleNominator,
      int quality) throws IOException;

  /**
   * Downscales and rotates jpeg image
   *
   * @param inputStream
   * @param outputStream
   * @param exifOrientation 0, 90, 180 or 270
   * @param scaleNumerator 1 - 16, image will be scaled using scaleNumerator/8 factor
   * @param quality 1 - 100
   */
  public static void transcodeJpegWithExifOrientation(
      final InputStream inputStream,
      final OutputStream outputStream,
      final int exifOrientation,
      final int scaleNumerator,
      final int quality)
      throws IOException {
    Preconditions.checkArgument(scaleNumerator >= MIN_SCALE_NUMERATOR);
    Preconditions.checkArgument(scaleNumerator <= MAX_SCALE_NUMERATOR);
    Preconditions.checkArgument(quality >= MIN_QUALITY);
    Preconditions.checkArgument(quality <= MAX_QUALITY);
    Preconditions.checkArgument(isExifOrientationAllowed(exifOrientation));
    Preconditions.checkArgument(
        scaleNumerator != SCALE_DENOMINATOR || exifOrientation != ExifInterface.ORIENTATION_NORMAL,
        "no transformation requested");
    nativeTranscodeJpegWithExifOrientation(
        Preconditions.checkNotNull(inputStream),
        Preconditions.checkNotNull(outputStream),
        exifOrientation,
        scaleNumerator,
        quality);
  }

  @DoNotStrip
  private static native void nativeTranscodeJpegWithExifOrientation(
      InputStream inputStream,
      OutputStream outputStream,
      int exifOrientation,
      int scaleNominator,
      int quality)
      throws IOException;
}

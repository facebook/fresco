/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.nativecode;

import static com.facebook.imagepipeline.transcoder.JpegTranscoderUtils.MAX_QUALITY;
import static com.facebook.imagepipeline.transcoder.JpegTranscoderUtils.MAX_SCALE_NUMERATOR;
import static com.facebook.imagepipeline.transcoder.JpegTranscoderUtils.MIN_QUALITY;
import static com.facebook.imagepipeline.transcoder.JpegTranscoderUtils.MIN_SCALE_NUMERATOR;
import static com.facebook.imagepipeline.transcoder.JpegTranscoderUtils.SCALE_DENOMINATOR;

import android.media.ExifInterface;
import com.facebook.common.internal.DoNotStrip;
import com.facebook.common.internal.Preconditions;
import com.facebook.imagepipeline.transcoder.JpegTranscoderUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** Transcoder for jpeg images, using native code and libjpeg-turbo library. */
@DoNotStrip
public class NativeJpegTranscoder {

  static {
    ImagePipelineNativeLoader.load();
  }

  /**
   * Transcodes an image to match the specified rotation angle and the scale factor.
   *
   * @param inputStream The {@link InputStream} of the image that will be transcoded.
   * @param outputStream The {@link OutputStream} where the newly created image is written to.
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
    Preconditions.checkArgument(JpegTranscoderUtils.isRotationAngleAllowed(rotationAngle));
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
      int quality)
      throws IOException;

  /**
   * Transcodes an image to match the specified exif orientation and the scale factor.
   *
   * @param inputStream The {@link InputStream} of the image that will be transcoded.
   * @param outputStream The {@link OutputStream} where the newly created image is written to.
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
    Preconditions.checkArgument(JpegTranscoderUtils.isExifOrientationAllowed(exifOrientation));
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

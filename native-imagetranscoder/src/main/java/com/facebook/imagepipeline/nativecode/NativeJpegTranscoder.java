/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.nativecode;

import static com.facebook.imagepipeline.transcoder.JpegTranscoderUtils.DEFAULT_JPEG_QUALITY;
import static com.facebook.imagepipeline.transcoder.JpegTranscoderUtils.INVERTED_EXIF_ORIENTATIONS;
import static com.facebook.imagepipeline.transcoder.JpegTranscoderUtils.MAX_QUALITY;
import static com.facebook.imagepipeline.transcoder.JpegTranscoderUtils.MAX_SCALE_NUMERATOR;
import static com.facebook.imagepipeline.transcoder.JpegTranscoderUtils.MIN_QUALITY;
import static com.facebook.imagepipeline.transcoder.JpegTranscoderUtils.MIN_SCALE_NUMERATOR;
import static com.facebook.imagepipeline.transcoder.JpegTranscoderUtils.SCALE_DENOMINATOR;

import android.media.ExifInterface;
import com.facebook.common.internal.Closeables;
import com.facebook.common.internal.DoNotStrip;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.imageformat.DefaultImageFormats;
import com.facebook.imageformat.ImageFormat;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.common.RotationOptions;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.transcoder.DownsampleUtil;
import com.facebook.imagepipeline.transcoder.ImageTranscodeResult;
import com.facebook.imagepipeline.transcoder.ImageTranscoder;
import com.facebook.imagepipeline.transcoder.JpegTranscoderUtils;
import com.facebook.imagepipeline.transcoder.TranscodeStatus;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.annotation.Nullable;

/** Transcoder for jpeg images, using native code and libjpeg-turbo library. */
@DoNotStrip
public class NativeJpegTranscoder implements ImageTranscoder {
  public static final String TAG = "NativeJpegTranscoder";

  private boolean mResizingEnabled;
  private int mMaxBitmapSize;
  private boolean mUseDownsamplingRatio;

  static {
    NativeJpegTranscoderSoLoader.ensure();
  }

  public NativeJpegTranscoder(
      final boolean resizingEnabled, final int maxBitmapSize, final boolean useDownsamplingRatio) {
    mResizingEnabled = resizingEnabled;
    mMaxBitmapSize = maxBitmapSize;
    mUseDownsamplingRatio = useDownsamplingRatio;
  }

  @Override
  public boolean canResize(
      EncodedImage encodedImage,
      @Nullable RotationOptions rotationOptions,
      @Nullable ResizeOptions resizeOptions) {
    if (rotationOptions == null) {
      rotationOptions = RotationOptions.autoRotate();
    }
    return JpegTranscoderUtils.getSoftwareNumerator(
            rotationOptions, resizeOptions, encodedImage, mResizingEnabled)
        < JpegTranscoderUtils.SCALE_DENOMINATOR;
  }

  @Override
  public boolean canTranscode(ImageFormat imageFormat) {
    return imageFormat == DefaultImageFormats.JPEG;
  }

  @Override
  public String getIdentifier() {
    return TAG;
  }

  @Override
  public ImageTranscodeResult transcode(
      final EncodedImage encodedImage,
      final OutputStream outputStream,
      @Nullable RotationOptions rotationOptions,
      @Nullable final ResizeOptions resizeOptions,
      @Nullable ImageFormat outputFormat,
      @Nullable Integer quality)
      throws IOException {
    if (quality == null) {
      quality = DEFAULT_JPEG_QUALITY;
    }
    if (rotationOptions == null) {
      rotationOptions = RotationOptions.autoRotate();
    }
    final int downsampleRatio =
        DownsampleUtil.determineSampleSize(
            rotationOptions, resizeOptions, encodedImage, mMaxBitmapSize);
    InputStream is = null;
    try {
      final int softwareNumerator =
          JpegTranscoderUtils.getSoftwareNumerator(
              rotationOptions, resizeOptions, encodedImage, mResizingEnabled);
      final int downsampleNumerator =
          JpegTranscoderUtils.calculateDownsampleNumerator(downsampleRatio);
      final int numerator;
      if (mUseDownsamplingRatio) {
        numerator = downsampleNumerator;
      } else {
        numerator = softwareNumerator;
      }
      is = encodedImage.getInputStream();
      if (INVERTED_EXIF_ORIENTATIONS.contains(encodedImage.getExifOrientation())) {
        // Use exif orientation to rotate since we can't use the rotation angle for
        // inverted exif orientations
        final int exifOrientation =
            JpegTranscoderUtils.getForceRotatedInvertedExifOrientation(
                rotationOptions, encodedImage);
        transcodeJpegWithExifOrientation(is, outputStream, exifOrientation, numerator, quality);
      } else {
        // Use actual rotation angle in degrees to rotate
        final int rotationAngle =
            JpegTranscoderUtils.getRotationAngle(rotationOptions, encodedImage);
        transcodeJpeg(is, outputStream, rotationAngle, numerator, quality);
      }
    } finally {
      Closeables.closeQuietly(is);
    }
    return new ImageTranscodeResult(
        downsampleRatio == DownsampleUtil.DEFAULT_SAMPLE_SIZE
            ? TranscodeStatus.TRANSCODING_NO_RESIZING
            : TranscodeStatus.TRANSCODING_SUCCESS);
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
  @VisibleForTesting
  public static void transcodeJpeg(
      final InputStream inputStream,
      final OutputStream outputStream,
      final int rotationAngle,
      final int scaleNumerator,
      final int quality)
      throws IOException {
    NativeJpegTranscoderSoLoader.ensure();
    Preconditions.checkArgument(scaleNumerator >= MIN_SCALE_NUMERATOR);
    Preconditions.checkArgument(scaleNumerator <= MAX_SCALE_NUMERATOR);
    Preconditions.checkArgument(quality >= MIN_QUALITY);
    Preconditions.checkArgument(quality <= MAX_QUALITY);
    Preconditions.checkArgument(JpegTranscoderUtils.isRotationAngleAllowed(rotationAngle));
    Preconditions.checkArgument(
        scaleNumerator != SCALE_DENOMINATOR || rotationAngle != 0, "no transformation requested");
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
  @VisibleForTesting
  public static void transcodeJpegWithExifOrientation(
      final InputStream inputStream,
      final OutputStream outputStream,
      final int exifOrientation,
      final int scaleNumerator,
      final int quality)
      throws IOException {
    NativeJpegTranscoderSoLoader.ensure();
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

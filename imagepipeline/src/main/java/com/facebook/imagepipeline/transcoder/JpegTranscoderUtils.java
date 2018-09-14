/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.imagepipeline.transcoder;

import android.graphics.Matrix;
import android.media.ExifInterface;
import com.facebook.common.internal.ImmutableList;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.common.RotationOptions;
import com.facebook.imagepipeline.image.EncodedImage;
import javax.annotation.Nullable;

public class JpegTranscoderUtils {

  private static final int FULL_ROUND = 360;

  public static final int MIN_QUALITY = 0;
  public static final int MAX_QUALITY = 100;
  public static final int MIN_SCALE_NUMERATOR = 1;
  public static final int MAX_SCALE_NUMERATOR = 16;
  public static final int SCALE_DENOMINATOR = 8;

  // Inverted EXIF orientations in clockwise rotation order. Rotating 90 degrees clockwise gets you
  // the next item in the list
  public static final ImmutableList<Integer> INVERTED_EXIF_ORIENTATIONS =
      ImmutableList.of(
          ExifInterface.ORIENTATION_FLIP_HORIZONTAL,
          ExifInterface.ORIENTATION_TRANSVERSE,
          ExifInterface.ORIENTATION_FLIP_VERTICAL,
          ExifInterface.ORIENTATION_TRANSPOSE);

  @VisibleForTesting public static final int DEFAULT_JPEG_QUALITY = 85;

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

  public static int getSoftwareNumerator(
      RotationOptions rotationOptions,
      @Nullable ResizeOptions resizeOptions,
      EncodedImage encodedImage,
      boolean resizingEnabled) {
    if (!resizingEnabled) {
      return SCALE_DENOMINATOR;
    }
    if (resizeOptions == null) {
      return SCALE_DENOMINATOR;
    }

    final int rotationAngle = getRotationAngle(rotationOptions, encodedImage);
    int exifOrientation = ExifInterface.ORIENTATION_UNDEFINED;
    if (INVERTED_EXIF_ORIENTATIONS.contains(encodedImage.getExifOrientation())) {
      exifOrientation = getForceRotatedInvertedExifOrientation(rotationOptions, encodedImage);
    }

    final boolean swapDimensions =
        rotationAngle == 90
            || rotationAngle == 270
            || exifOrientation == ExifInterface.ORIENTATION_TRANSPOSE
            || exifOrientation == ExifInterface.ORIENTATION_TRANSVERSE;
    final int widthAfterRotation =
        swapDimensions ? encodedImage.getHeight() : encodedImage.getWidth();
    final int heightAfterRotation =
        swapDimensions ? encodedImage.getWidth() : encodedImage.getHeight();

    float ratio = determineResizeRatio(resizeOptions, widthAfterRotation, heightAfterRotation);
    int numerator = roundNumerator(ratio, resizeOptions.roundUpFraction);
    if (numerator > SCALE_DENOMINATOR) {
      return SCALE_DENOMINATOR;
    }
    return (numerator < 1) ? 1 : numerator;
  }

  public static int getRotationAngle(RotationOptions rotationOptions, EncodedImage encodedImage) {
    if (!rotationOptions.rotationEnabled()) {
      return RotationOptions.NO_ROTATION;
    }
    int rotationFromMetadata = extractOrientationFromMetadata(encodedImage);
    if (rotationOptions.useImageMetadata()) {
      return rotationFromMetadata;
    }
    return (rotationFromMetadata + rotationOptions.getForcedAngle()) % FULL_ROUND;
  }

  /**
   * Get an inverted exif orientation (2, 4, 5, 7) but adjusted to take the force rotation angle
   * into consideration
   *
   * @throws IllegalArgumentException if encoded image passed doesn't have an inverted EXIF
   *     orientation
   */
  public static int getForceRotatedInvertedExifOrientation(
      RotationOptions rotationOptions, EncodedImage encodedImage) {
    int exifOrientation = encodedImage.getExifOrientation();
    int index = INVERTED_EXIF_ORIENTATIONS.indexOf(exifOrientation);
    if (index < 0) {
      throw new IllegalArgumentException("Only accepts inverted exif orientations");
    }
    int forcedAngle = RotationOptions.NO_ROTATION;
    if (!rotationOptions.useImageMetadata()) {
      forcedAngle = rotationOptions.getForcedAngle();
    }
    int timesToRotate = forcedAngle / 90;
    return INVERTED_EXIF_ORIENTATIONS.get(
        (index + timesToRotate) % INVERTED_EXIF_ORIENTATIONS.size());
  }

  @VisibleForTesting
  public static float determineResizeRatio(ResizeOptions resizeOptions, int width, int height) {
    if (resizeOptions == null) {
      return 1.0f;
    }

    final float widthRatio = ((float) resizeOptions.width) / width;
    final float heightRatio = ((float) resizeOptions.height) / height;
    float ratio = Math.max(widthRatio, heightRatio);

    if (width * ratio > resizeOptions.maxBitmapSize) {
      ratio = resizeOptions.maxBitmapSize / width;
    }
    if (height * ratio > resizeOptions.maxBitmapSize) {
      ratio = resizeOptions.maxBitmapSize / height;
    }
    return ratio;
  }

  @VisibleForTesting
  public static int roundNumerator(float maxRatio, float roundUpFraction) {
    return (int) (roundUpFraction + maxRatio * SCALE_DENOMINATOR);
  }

  /**
   * This method calculate the ratio in case the downsampling was enabled
   *
   * @param downsampleRatio The ratio from downsampling
   * @return The ratio to use for software resize using the downsampling limitation
   */
  @VisibleForTesting
  public static int calculateDownsampleNumerator(int downsampleRatio) {
    return Math.max(1, SCALE_DENOMINATOR / downsampleRatio);
  }

  /**
   * Compute the transformation matrix needed to rotate the image. If no transformation is needed,
   * it returns null.
   *
   * @param encodedImage The {@link EncodedImage} used when computing the matrix.
   * @param rotationOptions The {@link RotationOptions} used when computing the matrix
   * @return The transformation matrix, or null if no transformation is required.
   */
  @Nullable
  public static Matrix getTransformationMatrix(
      final EncodedImage encodedImage, final RotationOptions rotationOptions) {
    Matrix transformationMatrix = null;

    if (JpegTranscoderUtils.INVERTED_EXIF_ORIENTATIONS.contains(
        encodedImage.getExifOrientation())) {
      // Use exif orientation to rotate since we can't use the rotation angle for inverted exif
      // orientations
      final int exifOrientation =
          getForceRotatedInvertedExifOrientation(rotationOptions, encodedImage);
      transformationMatrix = getTransformationMatrixFromInvertedExif(exifOrientation);
    } else {
      // Use actual rotation angle in degrees to rotate
      final int rotationAngle = getRotationAngle(rotationOptions, encodedImage);
      if (rotationAngle != 0) {
        transformationMatrix = new Matrix();
        transformationMatrix.setRotate(rotationAngle);
      }
    }
    return transformationMatrix;
  }

  /**
   * Returns the transformation matrix if the orientation corresponds to one present in {@link
   * #INVERTED_EXIF_ORIENTATIONS}, else null.
   *
   * @param orientation the exif orientation
   * @return the transformation matrix if inverted orientation, else null.
   */
  @Nullable
  private static Matrix getTransformationMatrixFromInvertedExif(final int orientation) {
    Matrix matrix = new Matrix();
    switch (orientation) {
      case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
        matrix.setScale(-1, 1);
        break;
      case ExifInterface.ORIENTATION_TRANSVERSE:
        matrix.setRotate(-90);
        matrix.postScale(-1, 1);
        break;
      case ExifInterface.ORIENTATION_FLIP_VERTICAL:
        matrix.setRotate(180);
        matrix.postScale(-1, 1);
        break;
      case ExifInterface.ORIENTATION_TRANSPOSE:
        matrix.setRotate(90);
        matrix.postScale(-1, 1);
        break;
      default:
        return null;
    }
    return matrix;
  }

  private static int extractOrientationFromMetadata(EncodedImage encodedImage) {
    switch (encodedImage.getRotationAngle()) {
      case RotationOptions.ROTATE_90:
      case RotationOptions.ROTATE_180:
      case RotationOptions.ROTATE_270:
        return encodedImage.getRotationAngle();
      default:
        return 0;
    }
  }
}

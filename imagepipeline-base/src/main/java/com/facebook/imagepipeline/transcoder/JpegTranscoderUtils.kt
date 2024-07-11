/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.transcoder

import android.graphics.Matrix
import android.media.ExifInterface
import androidx.annotation.VisibleForTesting
import com.facebook.common.internal.ImmutableList
import com.facebook.imagepipeline.common.ResizeOptions
import com.facebook.imagepipeline.common.RotationOptions
import com.facebook.imagepipeline.image.EncodedImage

object JpegTranscoderUtils {

  private const val FULL_ROUND = 360
  const val MIN_QUALITY = 0
  const val MAX_QUALITY = 100
  const val MIN_SCALE_NUMERATOR = 1
  const val MAX_SCALE_NUMERATOR = 16
  const val SCALE_DENOMINATOR = 8

  /**
   * Inverted EXIF orientations in clockwise rotation order. Rotating 90 degrees clockwise gets you
   * the next item in the list
   */
  @JvmField //
  val INVERTED_EXIF_ORIENTATIONS: ImmutableList<Int> =
      ImmutableList.of(
          ExifInterface.ORIENTATION_FLIP_HORIZONTAL,
          ExifInterface.ORIENTATION_TRANSVERSE,
          ExifInterface.ORIENTATION_FLIP_VERTICAL,
          ExifInterface.ORIENTATION_TRANSPOSE)

  @VisibleForTesting const val DEFAULT_JPEG_QUALITY = 85

  /**
   * @return true if and only if given number of degrees is allowed rotation angle, that is it is
   *   equal to 0, 90, 180 or 270
   */
  @JvmStatic
  fun isRotationAngleAllowed(degrees: Int): Boolean =
      degrees >= 0 && degrees <= 270 && degrees % 90 == 0

  /** @return true if and only if given value is a valid EXIF orientation */
  @JvmStatic
  fun isExifOrientationAllowed(exifOrientation: Int): Boolean =
      when (exifOrientation) {
        ExifInterface.ORIENTATION_NORMAL,
        ExifInterface.ORIENTATION_ROTATE_90,
        ExifInterface.ORIENTATION_ROTATE_180,
        ExifInterface.ORIENTATION_ROTATE_270,
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL,
        ExifInterface.ORIENTATION_FLIP_VERTICAL,
        ExifInterface.ORIENTATION_TRANSPOSE,
        ExifInterface.ORIENTATION_TRANSVERSE -> true
        else -> false
      }

  @JvmStatic
  fun getSoftwareNumerator(
      rotationOptions: RotationOptions,
      resizeOptions: ResizeOptions?,
      encodedImage: EncodedImage,
      resizingEnabled: Boolean
  ): Int {
    if (!resizingEnabled) {
      return SCALE_DENOMINATOR
    }
    if (resizeOptions == null) {
      return SCALE_DENOMINATOR
    }
    val rotationAngle = getRotationAngle(rotationOptions, encodedImage)
    var exifOrientation = ExifInterface.ORIENTATION_UNDEFINED
    if (INVERTED_EXIF_ORIENTATIONS.contains(encodedImage.exifOrientation)) {
      exifOrientation = getForceRotatedInvertedExifOrientation(rotationOptions, encodedImage)
    }
    val swapDimensions =
        rotationAngle == 90 ||
            rotationAngle == 270 ||
            exifOrientation == ExifInterface.ORIENTATION_TRANSPOSE ||
            exifOrientation == ExifInterface.ORIENTATION_TRANSVERSE
    val widthAfterRotation = if (swapDimensions) encodedImage.height else encodedImage.width
    val heightAfterRotation = if (swapDimensions) encodedImage.width else encodedImage.height
    val ratio = determineResizeRatio(resizeOptions, widthAfterRotation, heightAfterRotation)
    val numerator = roundNumerator(ratio, resizeOptions.roundUpFraction)
    if (numerator > SCALE_DENOMINATOR) {
      return SCALE_DENOMINATOR
    }
    return if (numerator < 1) 1 else numerator
  }

  @JvmStatic
  fun getRotationAngle(rotationOptions: RotationOptions, encodedImage: EncodedImage): Int {
    if (!rotationOptions.rotationEnabled()) {
      return RotationOptions.NO_ROTATION
    }
    val rotationFromMetadata = extractOrientationFromMetadata(encodedImage)
    return if (rotationOptions.useImageMetadata()) {
      rotationFromMetadata
    } else {
      (rotationFromMetadata + rotationOptions.forcedAngle) % FULL_ROUND
    }
  }

  /**
   * Get an inverted exif orientation (2, 4, 5, 7) but adjusted to take the force rotation angle
   * into consideration
   *
   * @throws IllegalArgumentException if encoded image passed doesn't have an inverted EXIF
   *   orientation
   */
  @JvmStatic
  fun getForceRotatedInvertedExifOrientation(
      rotationOptions: RotationOptions,
      encodedImage: EncodedImage
  ): Int {
    val exifOrientation = encodedImage.exifOrientation
    val index = INVERTED_EXIF_ORIENTATIONS.indexOf(exifOrientation)
    require(index >= 0) { "Only accepts inverted exif orientations" }
    var forcedAngle = RotationOptions.NO_ROTATION
    if (!rotationOptions.useImageMetadata()) {
      forcedAngle = rotationOptions.forcedAngle
    }
    val timesToRotate = forcedAngle / 90
    return INVERTED_EXIF_ORIENTATIONS[(index + timesToRotate) % INVERTED_EXIF_ORIENTATIONS.size]
  }

  @JvmStatic
  @VisibleForTesting
  fun determineResizeRatio(resizeOptions: ResizeOptions?, width: Int, height: Int): Float {
    if (resizeOptions == null) {
      return 1.0f
    }
    val widthRatio = resizeOptions.width.toFloat() / width
    val heightRatio = resizeOptions.height.toFloat() / height
    var ratio = Math.max(widthRatio, heightRatio)
    if (width * ratio > resizeOptions.maxBitmapDimension) {
      ratio = resizeOptions.maxBitmapDimension / width
    }
    if (height * ratio > resizeOptions.maxBitmapDimension) {
      ratio = resizeOptions.maxBitmapDimension / height
    }
    return ratio
  }

  @JvmStatic
  @VisibleForTesting
  fun roundNumerator(maxRatio: Float, roundUpFraction: Float): Int =
      (roundUpFraction + maxRatio * SCALE_DENOMINATOR).toInt()

  /**
   * This method calculate the ratio in case the downsampling was enabled
   *
   * @param downsampleRatio The ratio from downsampling
   * @return The ratio to use for software resize using the downsampling limitation
   */
  @JvmStatic
  @VisibleForTesting
  fun calculateDownsampleNumerator(downsampleRatio: Int): Int =
      Math.max(1, SCALE_DENOMINATOR / downsampleRatio)

  /**
   * Compute the transformation matrix needed to rotate the image. If no transformation is needed,
   * it returns null.
   *
   * @param encodedImage The [EncodedImage] used when computing the matrix.
   * @param rotationOptions The [RotationOptions] used when computing the matrix
   * @return The transformation matrix, or null if no transformation is required.
   */
  @JvmStatic
  fun getTransformationMatrix(
      encodedImage: EncodedImage,
      rotationOptions: RotationOptions
  ): Matrix? {
    var transformationMatrix: Matrix? = null
    if (INVERTED_EXIF_ORIENTATIONS.contains(encodedImage.exifOrientation)) {
      // Use exif orientation to rotate since we can't use the rotation angle for inverted exif
      // orientations
      val exifOrientation = getForceRotatedInvertedExifOrientation(rotationOptions, encodedImage)
      transformationMatrix = getTransformationMatrixFromInvertedExif(exifOrientation)
    } else {
      // Use actual rotation angle in degrees to rotate
      val rotationAngle = getRotationAngle(rotationOptions, encodedImage)
      if (rotationAngle != 0) {
        transformationMatrix = Matrix()
        transformationMatrix.setRotate(rotationAngle.toFloat())
      }
    }
    return transformationMatrix
  }

  /**
   * Returns the transformation matrix if the orientation corresponds to one present in
   * [ ][.INVERTED_EXIF_ORIENTATIONS], else null.
   *
   * @param orientation the exif orientation
   * @return the transformation matrix if inverted orientation, else null.
   */
  private fun getTransformationMatrixFromInvertedExif(orientation: Int): Matrix? {
    val matrix = Matrix()
    when (orientation) {
      ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
      ExifInterface.ORIENTATION_TRANSVERSE -> {
        matrix.setRotate(-90f)
        matrix.postScale(-1f, 1f)
      }
      ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
        matrix.setRotate(180f)
        matrix.postScale(-1f, 1f)
      }
      ExifInterface.ORIENTATION_TRANSPOSE -> {
        matrix.setRotate(90f)
        matrix.postScale(-1f, 1f)
      }
      else -> return null
    }
    return matrix
  }

  private fun extractOrientationFromMetadata(encodedImage: EncodedImage): Int =
      when (encodedImage.rotationAngle) {
        RotationOptions.ROTATE_90,
        RotationOptions.ROTATE_180,
        RotationOptions.ROTATE_270 -> encodedImage.rotationAngle
        else -> 0
      }
}

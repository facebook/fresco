/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.common

import androidx.annotation.IntDef
import com.facebook.common.util.HashCodeUtil
import java.util.Locale

/**
 * Options for rotation.
 *
 * These options are applied to JPEG images only.
 *
 * Describes how the image should be rotated, whether following image meta-data or a specified
 * amount.
 *
 * These options are only relevant for JPEG images. Fresco doesn't support rotation of other image
 * formats.
 *
 * The options also include whether the rotation can be deferred until the bitmap is rendered. This
 * should be be false if a post-processor is used which needs to operate on the bitmap correctly
 * oriented but can otherwise generally be true, particularly if using drawee.
 */
class RotationOptions
private constructor(
    @field:Rotation @param:Rotation private val rotation: Int,
    private val deferUntilRendered: Boolean
) {

  @IntDef(
      flag = false,
      value =
          [
              NO_ROTATION,
              ROTATE_90,
              ROTATE_180,
              ROTATE_270,
              USE_EXIF_ROTATION_ANGLE,
              DISABLE_ROTATION])
  @Retention(AnnotationRetention.SOURCE)
  private annotation class Rotation

  @IntDef(flag = false, value = [NO_ROTATION, ROTATE_90, ROTATE_180, ROTATE_270])
  @Retention(AnnotationRetention.SOURCE)
  annotation class RotationAngle

  fun useImageMetadata(): Boolean = rotation == USE_EXIF_ROTATION_ANGLE

  fun rotationEnabled(): Boolean = rotation != DISABLE_ROTATION

  /**
   * Gets the explicit angle to rotate to, if one was set.
   *
   * @throws IllegalStateException if the instance was create using one of the `autoRotate()`
   *   constructors.
   */
  @get:RotationAngle
  val forcedAngle: Int
    get() {
      check(!useImageMetadata()) { "Rotation is set to use EXIF" }
      return rotation
    }

  fun canDeferUntilRendered(): Boolean = deferUntilRendered

  override fun hashCode(): Int = HashCodeUtil.hashCode(rotation, deferUntilRendered)

  override fun equals(other: Any?): Boolean {
    if (other === this) {
      return true
    }
    if (other !is RotationOptions) {
      return false
    }
    val that = other
    return rotation == that.rotation && deferUntilRendered == that.deferUntilRendered
  }

  override fun toString(): String =
      String.format(null as Locale?, "%d defer:%b", rotation, deferUntilRendered)

  companion object {
    const val NO_ROTATION = 0
    const val ROTATE_90 = 90
    const val ROTATE_180 = 180
    const val ROTATE_270 = 270
    private const val USE_EXIF_ROTATION_ANGLE = -1
    private const val DISABLE_ROTATION = -2
    private val ROTATION_OPTIONS_AUTO_ROTATE = RotationOptions(USE_EXIF_ROTATION_ANGLE, false)
    private val ROTATION_OPTIONS_DISABLE_ROTATION = RotationOptions(DISABLE_ROTATION, false)
    private val ROTATION_OPTIONS_ROTATE_AT_RENDER_TIME =
        RotationOptions(USE_EXIF_ROTATION_ANGLE, true)

    /**
     * Creates a new set of rotation options for JPEG images to use the rotation angle in the image
     * metadata.
     *
     * This is the default option for requests which don't specify rotation options.
     *
     * The rotation will not be deferred for defensiveness but that can improve performance. To
     * defer, use [autoRotateAtRenderTime()].
     */
    @JvmStatic fun autoRotate(): RotationOptions = ROTATION_OPTIONS_AUTO_ROTATE

    /** Creates a new set of rotation options for JPEG images to load image without any rotation. */
    @JvmStatic fun disableRotation(): RotationOptions = ROTATION_OPTIONS_DISABLE_ROTATION

    /**
     * Creates a new set of rotation options for JPEG images to use the rotation angle in the image
     * metadata.
     *
     * The rotation may be deferred until the image is rendered.
     */
    @JvmStatic
    fun autoRotateAtRenderTime(): RotationOptions = ROTATION_OPTIONS_ROTATE_AT_RENDER_TIME

    /**
     * Creates a new set of rotation options to use a specific rotation angle.
     *
     * The rotation will be carried out in the pipeline.
     *
     * @param angle the angle to rotate - valid values are 0, 90, 180 and 270
     */
    @JvmStatic
    fun forceRotation(@RotationAngle angle: Int): RotationOptions = RotationOptions(angle, false)
  }
}

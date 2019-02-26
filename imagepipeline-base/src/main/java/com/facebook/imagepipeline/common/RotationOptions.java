/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.common;

import androidx.annotation.IntDef;
import com.facebook.common.util.HashCodeUtil;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Locale;

/**
 * Options for rotation.
 *
 * <p> These options are applied to JPEG images only.
 *
 * <p> Describes how the image should be rotated, whether following image meta-data or a specified
 * amount.
 *
 * <p> These options are only relevant for JPEG images. Fresco doesn't support rotation of other
 * image formats.
 *
 * <p> The options also include whether the rotation can be deferred until the bitmap is rendered.
 * This should be be false if a post-processor is used which needs to operate on the bitmap
 * correctly oriented but can otherwise generally be true, particularly if using drawee.
 */
public class RotationOptions {

  @IntDef(flag=false, value={
      NO_ROTATION,
      ROTATE_90,
      ROTATE_180,
      ROTATE_270,
      USE_EXIF_ROTATION_ANGLE,
      DISABLE_ROTATION
  })
  @Retention(RetentionPolicy.SOURCE)
  private @interface Rotation {}

  @IntDef(flag=false, value={
      NO_ROTATION,
      ROTATE_90,
      ROTATE_180,
      ROTATE_270
  })
  @Retention(RetentionPolicy.SOURCE)
  public @interface RotationAngle {}

  public static final int NO_ROTATION = 0;
  public static final int ROTATE_90 = 90;
  public static final int ROTATE_180 = 180;
  public static final int ROTATE_270 = 270;
  private static final int USE_EXIF_ROTATION_ANGLE = -1;
  private static final int DISABLE_ROTATION = -2;

  private final @Rotation int mRotation;
  private final boolean mDeferUntilRendered;

  private static final RotationOptions ROTATION_OPTIONS_AUTO_ROTATE =
      new RotationOptions(USE_EXIF_ROTATION_ANGLE, false);

  private static final RotationOptions ROTATION_OPTIONS_DISABLE_ROTATION =
      new RotationOptions(DISABLE_ROTATION, false);

  private static final RotationOptions ROTATION_OPTIONS_ROTATE_AT_RENDER_TIME =
      new RotationOptions(USE_EXIF_ROTATION_ANGLE, true);

  /**
   * Creates a new set of rotation options for JPEG images to use the rotation angle in the image
   * metadata.
   *
   * <p> This is the default option for requests which don't specify rotation options.
   *
   * <p> The rotation will not be deferred for defensiveness but that can improve performance. To
   * defer, use {@link #autoRotateAtRenderTime()}.
   */
  public static RotationOptions autoRotate() {
    return ROTATION_OPTIONS_AUTO_ROTATE;
  }

  /**
   * Creates a new set of rotation options for JPEG images to load image without any rotation.
   *
   */
  public static RotationOptions disableRotation() {
    return ROTATION_OPTIONS_DISABLE_ROTATION;
  }

  /**
   * Creates a new set of rotation options for JPEG images to use the rotation angle in the image
   * metadata.
   *
   * <p> The rotation may be deferred until the image is rendered.
   */
  public static RotationOptions autoRotateAtRenderTime() {
    return ROTATION_OPTIONS_ROTATE_AT_RENDER_TIME;
  }

  /**
   * Creates a new set of rotation options to use a specific rotation angle.
   *
   * <p> The rotation will be carried out in the pipeline.
   *
   * @param angle the angle to rotate - valid values are 0, 90, 180 and 270
   */
  public static RotationOptions forceRotation(@RotationAngle int angle) {
    return new RotationOptions(angle, false);
  }

  private RotationOptions(@Rotation int rotation, boolean canDeferUntilRendered) {
    this.mRotation = rotation;
    this.mDeferUntilRendered = canDeferUntilRendered;
  }

  public boolean useImageMetadata() {
    return mRotation == USE_EXIF_ROTATION_ANGLE;
  }

  public boolean rotationEnabled() {
    return mRotation != DISABLE_ROTATION;
  }

  /**
   * Gets the explicit angle to rotate to, if one was set.
   *
   * @throws IllegalStateException if the instance was create using one of the
   * {@code autoRotate()} constructors.
   */
  public @RotationAngle int getForcedAngle() {
    if (useImageMetadata()) {
      throw new IllegalStateException("Rotation is set to use EXIF");
    }
    return mRotation;
  }

  public boolean canDeferUntilRendered() {
    return mDeferUntilRendered;
  }

  @Override
  public int hashCode() {
    return HashCodeUtil.hashCode(mRotation, mDeferUntilRendered);
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if (!(other instanceof RotationOptions)) {
      return false;
    }
    RotationOptions that = (RotationOptions) other;
    return this.mRotation == that.mRotation &&
        this.mDeferUntilRendered == that.mDeferUntilRendered;
  }

  @Override
  public String toString() {
    return String.format((Locale) null, "%d defer:%b", mRotation, mDeferUntilRendered);
  }
}

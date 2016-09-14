/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.common;

import android.support.annotation.IntDef;

import com.facebook.common.util.HashCodeUtil;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Locale;

/**
 * Options for rotation.
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
      USE_EXIF_ROTATION_ANGLE
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

  private final @Rotation int mRotation;
  private final boolean mDeferUntilRendered;

  /**
   * Creates a new set of rotation options to use the rotation angle in the image metadata.
   *
   * <p> The rotation may be deferred until render time.
   */
  public static RotationOptions createForImageMetadata() {
    return new RotationOptions(USE_EXIF_ROTATION_ANGLE, true);
  }

  /**
   * Creates a new set of rotation options to use the rotation angle in the image metadata.
   *
   * @param deferUntilRendered true if the rotation may be deferred until render time
   */
  public static RotationOptions createForImageMetadata(boolean deferUntilRendered) {
    return new RotationOptions(USE_EXIF_ROTATION_ANGLE, deferUntilRendered);
  }

  /**
   * Creates a new set of rotation options to use a specific rotation angle.
   *
   * <p> The rotation may be deferred until render time.
   */
  public static RotationOptions createForForcedRotation(@RotationAngle int angle) {
    return new RotationOptions(angle, true);
  }

  /**
   * Creates a new set of rotation options to use a specific rotation angle.
   *
   * @param deferUntilRendered true if the rotation may be deferred until render time
   */
  public static RotationOptions createForForcedRotation(
      @RotationAngle int angle,
      boolean deferUntilRendered) {
    return new RotationOptions(angle, true);
  }

  private RotationOptions(@Rotation int rotation, boolean deferUntilRendered) {
    this.mRotation = rotation;
    this.mDeferUntilRendered = deferUntilRendered;
  }

  public boolean useImageMetadata() {
    return mRotation == USE_EXIF_ROTATION_ANGLE;
  }

  /**
   * Gets the explicit angle to rotate to, if one was set.
   *
   * @throws IllegalStateException if the instance was create using one of the
   * {@code createForImageMetadata()} constructors.
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
    return String.format((Locale) null, "%d defer:%d", mRotation, mDeferUntilRendered);
  }
}

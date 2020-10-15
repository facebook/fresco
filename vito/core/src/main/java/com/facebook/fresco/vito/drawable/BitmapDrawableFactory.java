/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.drawable;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import com.facebook.drawee.drawable.OrientedDrawable;
import com.facebook.fresco.vito.core.FrescoExperiments;
import com.facebook.fresco.vito.options.BorderOptions;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.options.ImageOptionsDrawableFactory;
import com.facebook.fresco.vito.options.RoundingOptions;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.CloseableStaticBitmap;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.systrace.FrescoSystrace;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class BitmapDrawableFactory implements ImageOptionsDrawableFactory {

  private final Resources mResources;
  private final FrescoExperiments mExperiments;
  private final RoundingUtils mRoundingUtils;

  public BitmapDrawableFactory(Resources resources, FrescoExperiments frescoExperiments) {
    mResources = resources;
    mExperiments = frescoExperiments;
    mRoundingUtils = new RoundingUtils();
  }

  @Override
  @Nullable
  public Drawable createDrawable(CloseableImage closeableImage, ImageOptions imageOptions) {
    try {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.beginSection("BitmapDrawableFactory#createDrawable");
      }
      if (closeableImage instanceof CloseableStaticBitmap) {
        return handleCloseableStaticBitmap((CloseableStaticBitmap) closeableImage, imageOptions);
      }
      return null;
    } finally {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
  }

  /**
   * We handle the given bitmap and return a Drawable ready for being displayed: If rounding is set,
   * the image will be rounded, if a border if set, the border will be applied and finally, the
   * image will be rotated if required.
   *
   * <p>Bitmap -> border -> rounded corners -> RoundedBitmapDrawable (since bitmap is square) ->
   * fully circular -> CircularBorderBitmapDrawable (bitmap is circular) -> square image ->
   * RoundedBitmapDrawable (for border support) -> no border -> rounded corners ->
   * RoundedBitmapDrawable (since bitmap is square) -> fully circular -> BitmapDrawable (since
   * bitmap is circular) -> square image -> BitmapDrawable
   *
   * <p>If needed, the resulting drawable is rotated using OrientedDrawable.
   *
   * @param closeableStaticBitmap the image to handle
   * @param imageOptions display options for the given image
   * @return the drawable to display
   */
  protected Drawable handleCloseableStaticBitmap(
      CloseableStaticBitmap closeableStaticBitmap, ImageOptions imageOptions) {
    RoundingOptions roundingOptions = imageOptions.getRoundingOptions();
    BorderOptions borderOptions = imageOptions.getBorderOptions();

    boolean forceRoundAtDecode =
        roundingOptions == null ? false : roundingOptions.isForceRoundAtDecode();
    mRoundingUtils.setAlreadyRounded(!forceRoundAtDecode && mExperiments.useNativeRounding());

    return rotatedDrawable(
        closeableStaticBitmap,
        mRoundingUtils.roundedDrawable(
            mResources,
            closeableStaticBitmap.getUnderlyingBitmap(),
            borderOptions,
            roundingOptions));
  }

  protected Drawable rotatedDrawable(
      CloseableStaticBitmap closeableStaticBitmap, Drawable drawable) {
    if (!hasTransformableRotationAngle(closeableStaticBitmap)
        && !hasTransformableExifOrientation(closeableStaticBitmap)) {
      // Return the bitmap drawable directly as there's nothing to transform in it
      return drawable;
    } else {
      return new OrientedDrawable(
          drawable,
          closeableStaticBitmap.getRotationAngle(),
          closeableStaticBitmap.getExifOrientation());
    }
  }

  /* Returns true if there is anything to rotate using the rotation angle */
  private static boolean hasTransformableRotationAngle(
      CloseableStaticBitmap closeableStaticBitmap) {
    return closeableStaticBitmap.getRotationAngle() != 0
        && closeableStaticBitmap.getRotationAngle() != EncodedImage.UNKNOWN_ROTATION_ANGLE;
  }

  /* Returns true if there is anything to rotate using the EXIF orientation */
  private static boolean hasTransformableExifOrientation(
      CloseableStaticBitmap closeableStaticBitmap) {
    return closeableStaticBitmap.getExifOrientation() != ExifInterface.ORIENTATION_NORMAL
        && closeableStaticBitmap.getExifOrientation() != ExifInterface.ORIENTATION_UNDEFINED;
  }
}

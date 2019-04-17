/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.drawable;

import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import com.facebook.drawee.drawable.OrientedDrawable;
import com.facebook.drawee.drawable.RoundedBitmapDrawable;
import com.facebook.fresco.vito.options.BorderOptions;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.options.RoundingOptions;
import com.facebook.imagepipeline.drawable.DrawableFactory;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.CloseableStaticBitmap;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.systrace.FrescoSystrace;
import javax.annotation.Nullable;

public class VitoDrawableFactoryImpl implements VitoDrawableFactory {

  private final Resources mResources;
  private final @Nullable DrawableFactory mAnimatedDrawableFactory;

  public VitoDrawableFactoryImpl(
      Resources resources, @Nullable DrawableFactory animatedDrawableFactory) {
    mResources = resources;
    mAnimatedDrawableFactory = animatedDrawableFactory;
  }

  @Override
  @Nullable
  public Drawable createDrawable(CloseableImage closeableImage, ImageOptions imageOptions) {
    try {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.beginSection("VitoDrawableFactoryImpl#createDrawable");
      }
      if (closeableImage instanceof CloseableStaticBitmap) {
        return handleCloseableStaticBitmap((CloseableStaticBitmap) closeableImage, imageOptions);
      } else if (mAnimatedDrawableFactory != null
          && mAnimatedDrawableFactory.supportsImageType(closeableImage)) {
        return mAnimatedDrawableFactory.createDrawable(closeableImage);
      } else {
        return null;
      }
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

    if (borderOptions != null && borderOptions.width > 0) {
      return rotatedDrawable(
          closeableStaticBitmap,
          roundedDrawableWithBorder(closeableStaticBitmap, borderOptions, roundingOptions));
    } else {
      return rotatedDrawable(
          closeableStaticBitmap,
          roundedDrawableWithoutBorder(closeableStaticBitmap, roundingOptions));
    }
  }

  protected Drawable roundedDrawableWithoutBorder(
      CloseableStaticBitmap closeableStaticBitmap, @Nullable RoundingOptions roundingOptions) {
    // We need to return a rounded corner drawable if needed
    if (roundingOptions != null && !roundingOptions.isCircular()) {
      return roundedCornerDrawableWithBorder(closeableStaticBitmap, null, roundingOptions);
    }
    return new BitmapDrawable(mResources, closeableStaticBitmap.getUnderlyingBitmap());
  }

  protected Drawable roundedDrawableWithBorder(
      CloseableStaticBitmap closeableStaticBitmap,
      BorderOptions borderOptions,
      @Nullable RoundingOptions roundingOptions) {
    if (roundingOptions != null) {
      if (roundingOptions.isCircular()) {
        // Circular rounding is performed on the bitmap, so we only need to draw a circular border
        return circularDrawableWithBorder(closeableStaticBitmap, borderOptions);
      } else {
        return roundedCornerDrawableWithBorder(
            closeableStaticBitmap, borderOptions, roundingOptions);
      }
    } else {
      return squareDrawableWithBorder(closeableStaticBitmap, borderOptions);
    }
  }

  protected Drawable circularDrawableWithBorder(
      CloseableStaticBitmap closeableStaticBitmap, BorderOptions borderOptions) {
    CircularBorderBitmapDrawable drawable =
        new CircularBorderBitmapDrawable(mResources, closeableStaticBitmap.getUnderlyingBitmap());
    drawable.setBorder(borderOptions);
    return drawable;
  }

  protected Drawable roundedCornerDrawableWithBorder(
      CloseableStaticBitmap closeableStaticBitmap,
      @Nullable BorderOptions borderOptions,
      @Nullable RoundingOptions roundingOptions) {
    RoundedBitmapDrawable roundedBitmapDrawable =
        new RoundedBitmapDrawable(mResources, closeableStaticBitmap.getUnderlyingBitmap());
    if (roundingOptions != null) {
      float[] radii = roundingOptions.getCornerRadii();
      if (radii != null) {
        roundedBitmapDrawable.setRadii(radii);
      } else {
        roundedBitmapDrawable.setRadius(roundingOptions.getCornerRadius());
      }
    }
    if (borderOptions != null) {
      roundedBitmapDrawable.setBorder(borderOptions.color, borderOptions.width);
    }
    return roundedBitmapDrawable;
  }

  protected Drawable squareDrawableWithBorder(
      CloseableStaticBitmap closeableStaticBitmap, BorderOptions borderOptions) {
    // We use the same rounded corner drawable to draw the border without applying rounding
    return roundedCornerDrawableWithBorder(closeableStaticBitmap, borderOptions, null);
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

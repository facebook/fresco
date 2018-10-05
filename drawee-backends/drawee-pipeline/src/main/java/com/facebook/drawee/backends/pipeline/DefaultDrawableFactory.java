/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.backends.pipeline;

import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import com.facebook.drawee.drawable.OrientedDrawable;
import com.facebook.imagepipeline.drawable.DrawableFactory;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.CloseableStaticBitmap;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.systrace.FrescoSystrace;
import javax.annotation.Nullable;

public class DefaultDrawableFactory implements DrawableFactory {

  private final Resources mResources;
  private final @Nullable DrawableFactory mAnimatedDrawableFactory;

  public DefaultDrawableFactory(
      Resources resources, @Nullable DrawableFactory animatedDrawableFactory) {
    mResources = resources;
    mAnimatedDrawableFactory = animatedDrawableFactory;
  }

  @Override
  public boolean supportsImageType(CloseableImage image) {
    return true;
  }

  @Override
  @Nullable
  public Drawable createDrawable(CloseableImage closeableImage) {
    try {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.beginSection("DefaultDrawableFactory#createDrawable");
      }
      if (closeableImage instanceof CloseableStaticBitmap) {
        CloseableStaticBitmap closeableStaticBitmap = (CloseableStaticBitmap) closeableImage;
        Drawable bitmapDrawable =
            new BitmapDrawable(mResources, closeableStaticBitmap.getUnderlyingBitmap());
        if (!hasTransformableRotationAngle(closeableStaticBitmap)
            && !hasTransformableExifOrientation(closeableStaticBitmap)) {
          // Return the bitmap drawable directly as there's nothing to transform in it
          return bitmapDrawable;
        } else {
          return new OrientedDrawable(
              bitmapDrawable,
              closeableStaticBitmap.getRotationAngle(),
              closeableStaticBitmap.getExifOrientation());
        }
      } else if (mAnimatedDrawableFactory != null
          && mAnimatedDrawableFactory.supportsImageType(closeableImage)) {
        return mAnimatedDrawableFactory.createDrawable(closeableImage);
      }
      return null;
    } finally {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
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

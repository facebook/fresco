/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.backends.pipeline;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import com.facebook.drawee.drawable.OrientedDrawable;
import com.facebook.imagepipeline.drawable.DrawableFactory;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.CloseableStaticBitmap;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.systrace.FrescoSystrace;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class DefaultDrawableFactory implements DrawableFactory {

  private final Resources mResources;
  private final @Nullable DrawableFactory mAnimatedDrawableFactory;
  private final @Nullable DrawableFactory mXmlDrawableFactory;

  public DefaultDrawableFactory(
      Resources resources,
      @Nullable DrawableFactory animatedDrawableFactory,
      @Nullable DrawableFactory xmlDrawableFactory) {
    mResources = resources;
    mAnimatedDrawableFactory = animatedDrawableFactory;
    mXmlDrawableFactory = xmlDrawableFactory;
  }

  public DefaultDrawableFactory(
      Resources resources, @Nullable DrawableFactory animatedDrawableFactory) {
    this(resources, animatedDrawableFactory, null);
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
        Bitmap bitmap = closeableStaticBitmap.getUnderlyingBitmap();
        if (bitmap == null || bitmap.isRecycled()) {
          return null;
        }
        try {
          Drawable bitmapDrawable = new BitmapDrawable(mResources, bitmap);
          if (!hasTransformableRotationAngle(closeableStaticBitmap)
              && !hasTransformableExifOrientation(closeableStaticBitmap)) {
            return bitmapDrawable;
          } else {
            return new OrientedDrawable(
                bitmapDrawable,
                closeableStaticBitmap.getRotationAngle(),
                closeableStaticBitmap.getExifOrientation());
          }
        } catch (IllegalStateException e) {
          return null;
        }
      } else if (mAnimatedDrawableFactory != null
          && mAnimatedDrawableFactory.supportsImageType(closeableImage)) {
        return mAnimatedDrawableFactory.createDrawable(closeableImage);
      } else if (mXmlDrawableFactory != null
          && mXmlDrawableFactory.supportsImageType(closeableImage)) {
        return mXmlDrawableFactory.createDrawable(closeableImage);
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

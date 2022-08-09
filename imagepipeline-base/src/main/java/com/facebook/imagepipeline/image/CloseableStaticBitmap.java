/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.image;

import android.graphics.Bitmap;
import android.media.ExifInterface;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.references.ResourceReleaser;
import com.facebook.infer.annotation.Nullsafe;

@Nullsafe(Nullsafe.Mode.LOCAL)
public interface CloseableStaticBitmap extends CloseableBitmap {

  CloseableReference<Bitmap> cloneUnderlyingBitmapReference();

  int getExifOrientation();

  int getRotationAngle();

  CloseableReference<Bitmap> convertToBitmapReference();

  /**
   * Creates a new instance of a CloseableStaticBitmap.
   *
   * @param bitmap the bitmap to wrap
   * @param resourceReleaser ResourceReleaser to release the bitmap to
   */
  static CloseableStaticBitmap of(
      Bitmap bitmap,
      ResourceReleaser<Bitmap> resourceReleaser,
      QualityInfo qualityInfo,
      int rotationAngle) {
    return of(
        bitmap, resourceReleaser, qualityInfo, rotationAngle, ExifInterface.ORIENTATION_UNDEFINED);
  }

  /**
   * Creates a new instance of a CloseableStaticBitmap from an existing CloseableReference. The
   * CloseableStaticBitmap will hold a reference to the Bitmap until it's closed.
   *
   * @param bitmapReference the bitmap reference.
   */
  static CloseableStaticBitmap of(
      CloseableReference<Bitmap> bitmapReference, QualityInfo qualityInfo, int rotationAngle) {
    return of(bitmapReference, qualityInfo, rotationAngle, ExifInterface.ORIENTATION_UNDEFINED);
  }

  /**
   * Creates a new instance of a CloseableStaticBitmap from an existing CloseableReference. The
   * CloseableStaticBitmap will hold a reference to the Bitmap until it's closed.
   *
   * @param bitmap the bitmap to wrap
   * @param resourceReleaser ResourceReleaser to release the bitmap to
   */
  static CloseableStaticBitmap of(
      Bitmap bitmap,
      ResourceReleaser<Bitmap> resourceReleaser,
      QualityInfo qualityInfo,
      int rotationAngle,
      int orientation) {
    if (BaseCloseableStaticBitmap.shouldUseSimpleCloseableStaticBitmap()) {
      return new BaseCloseableStaticBitmap(
          bitmap, resourceReleaser, qualityInfo, rotationAngle, orientation);
    } else {
      return new DefaultCloseableStaticBitmap(
          bitmap, resourceReleaser, qualityInfo, rotationAngle, orientation);
    }
  }

  /**
   * Creates a new instance of a CloseableStaticBitmap from an existing CloseableReference. The
   * CloseableStaticBitmap will hold a reference to the Bitmap until it's closed.
   *
   * @param bitmapReference the bitmap reference.
   */
  static CloseableStaticBitmap of(
      CloseableReference<Bitmap> bitmapReference,
      QualityInfo qualityInfo,
      int rotationAngle,
      int exifOrientation) {
    if (BaseCloseableStaticBitmap.shouldUseSimpleCloseableStaticBitmap()) {
      return new BaseCloseableStaticBitmap(
          bitmapReference, qualityInfo, rotationAngle, exifOrientation);
    } else {
      return new DefaultCloseableStaticBitmap(
          bitmapReference, qualityInfo, rotationAngle, exifOrientation);
    }
  }
}

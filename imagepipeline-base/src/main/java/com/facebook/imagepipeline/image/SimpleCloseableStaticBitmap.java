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
public class SimpleCloseableStaticBitmap extends BaseCloseableStaticBitmap {
  private SimpleCloseableStaticBitmap(
      Bitmap bitmap,
      ResourceReleaser<Bitmap> resourceReleaser,
      QualityInfo qualityInfo,
      int rotationAngle,
      int exifOrientation) {
    super(bitmap, resourceReleaser, qualityInfo, rotationAngle, exifOrientation);
  }

  private SimpleCloseableStaticBitmap(
      CloseableReference<Bitmap> bitmapReference, QualityInfo qualityInfo, int rotationAngle) {
    super(bitmapReference, qualityInfo, rotationAngle, ExifInterface.ORIENTATION_UNDEFINED);
  }

  private SimpleCloseableStaticBitmap(
      CloseableReference<Bitmap> bitmapReference,
      QualityInfo qualityInfo,
      int rotationAngle,
      int exifOrientation) {
    super(bitmapReference, qualityInfo, rotationAngle, exifOrientation);
  }
}

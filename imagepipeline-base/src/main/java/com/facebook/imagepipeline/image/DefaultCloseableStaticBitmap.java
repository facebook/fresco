/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.image;

import android.graphics.Bitmap;
import com.facebook.common.logging.FLog;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.references.ResourceReleaser;
import com.facebook.infer.annotation.Nullsafe;

@Nullsafe(Nullsafe.Mode.LOCAL)
class DefaultCloseableStaticBitmap extends BaseCloseableStaticBitmap {

  private static final String TAG = "DefaultCloseableStaticBitmap";

  protected DefaultCloseableStaticBitmap(
      CloseableReference<Bitmap> bitmapReference,
      QualityInfo qualityInfo,
      int rotationAngle,
      int exifOrientation) {
    super(bitmapReference, qualityInfo, rotationAngle, exifOrientation);
  }

  protected DefaultCloseableStaticBitmap(
      Bitmap bitmap,
      ResourceReleaser<Bitmap> resourceReleaser,
      QualityInfo qualityInfo,
      int rotationAngle,
      int exifOrientation) {
    super(bitmap, resourceReleaser, qualityInfo, rotationAngle, exifOrientation);
  }

  /** Ensures that the underlying resources are always properly released. */
  @Override
  protected void finalize() throws Throwable {
    if (isClosed()) {
      return;
    }
    FLog.w(
        TAG,
        "finalize: %s %x still open.",
        this.getClass().getSimpleName(),
        System.identityHashCode(this));
    try {
      close();
    } finally {
      super.finalize();
    }
  }
}

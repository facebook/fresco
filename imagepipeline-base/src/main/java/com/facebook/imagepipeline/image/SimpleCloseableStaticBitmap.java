/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.image;

import android.graphics.Bitmap;
import com.facebook.common.references.CloseableReference;
import com.facebook.infer.annotation.Nullsafe;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class SimpleCloseableStaticBitmap extends BaseCloseableStaticBitmap {

  protected SimpleCloseableStaticBitmap(
      CloseableReference<Bitmap> bitmapReference,
      QualityInfo qualityInfo,
      int rotationAngle,
      int exifOrientation) {
    super(bitmapReference, qualityInfo, rotationAngle, exifOrientation);
  }
}

/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.decoder;

import android.graphics.Bitmap;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.CloseableStaticBitmap;
import com.facebook.imagepipeline.image.ImmutableQualityInfo;
import com.facebook.imagepipeline.nativecode.Bitmaps;

public class CloseableImageCopier {

  private final PlatformBitmapFactory mPlatformBitmapFactory;

  public CloseableImageCopier(
      PlatformBitmapFactory platformBitmapFactory) {
    mPlatformBitmapFactory = platformBitmapFactory;
  }

  public CloseableReference<CloseableImage> copyCloseableImage(
      CloseableReference<CloseableImage> closeableImageRef) {
    Preconditions.checkArgument(isCloseableImageCopyable(closeableImageRef));
    return copyCloseableStaticBitmap(closeableImageRef);
  }

  private CloseableReference<CloseableImage> copyCloseableStaticBitmap(
      final CloseableReference<CloseableImage> closeableStaticBitmapRef) {
    Bitmap sourceBitmap = ((CloseableStaticBitmap) closeableStaticBitmapRef.get())
        .getUnderlyingBitmap();
    CloseableReference<Bitmap> bitmapRef = mPlatformBitmapFactory.createBitmap(
        sourceBitmap.getWidth(),
        sourceBitmap.getHeight());
    try {
      Bitmap destinationBitmap = bitmapRef.get();
      Preconditions.checkState(!destinationBitmap.isRecycled());
      Preconditions.checkState(destinationBitmap.isMutable());
      Bitmaps.copyBitmap(destinationBitmap, sourceBitmap);
      return CloseableReference.<CloseableImage>of(
          new CloseableStaticBitmap(bitmapRef, ImmutableQualityInfo.FULL_QUALITY));
    } finally {
      bitmapRef.close();
    }
  }

  public boolean isCloseableImageCopyable(CloseableReference<CloseableImage> closeableImageRef) {
    return closeableImageRef != null && closeableImageRef.get() instanceof CloseableStaticBitmap;
  }

}

/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.bitmaps;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import com.facebook.common.logging.FLog;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.references.CloseableReference;
import com.facebook.imageformat.DefaultImageFormats;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.platform.PlatformDecoder;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Factory implementation for Honeycomb through Kitkat
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
@ThreadSafe
public class HoneycombBitmapFactory extends PlatformBitmapFactory {

  private static final String TAG = HoneycombBitmapFactory.class.getSimpleName();
  private final EmptyJpegGenerator mJpegGenerator;
  private final PlatformDecoder mPurgeableDecoder;
  private boolean mImmutableBitmapFallback;

  public HoneycombBitmapFactory(
      EmptyJpegGenerator jpegGenerator, PlatformDecoder purgeableDecoder) {
    mJpegGenerator = jpegGenerator;
    mPurgeableDecoder = purgeableDecoder;
  }

  /**
   * Creates a bitmap of the specified width and height.
   *
   * @param width the width of the bitmap
   * @param height the height of the bitmap
   * @param bitmapConfig the {@link android.graphics.Bitmap.Config}
   * used to create the decoded Bitmap
   * @return a reference to the bitmap
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
  @Override
  public CloseableReference<Bitmap> createBitmapInternal(
      int width,
      int height,
      Bitmap.Config bitmapConfig) {
    if (mImmutableBitmapFallback) {
      return createFallbackBitmap(width, height, bitmapConfig);
    }
    CloseableReference<PooledByteBuffer> jpgRef = mJpegGenerator.generate(
        (short) width,
        (short) height);
    try {
      EncodedImage encodedImage = new EncodedImage(jpgRef);
      encodedImage.setImageFormat(DefaultImageFormats.JPEG);
      try {
        CloseableReference<Bitmap> bitmapRef =
            mPurgeableDecoder.decodeJPEGFromEncodedImage(
                encodedImage, bitmapConfig, null, jpgRef.get().size());
        if (!bitmapRef.get().isMutable()) {
          CloseableReference.closeSafely(bitmapRef);
          mImmutableBitmapFallback = true;
          FLog.wtf(TAG, "Immutable bitmap returned by decoder");
          // On some devices (Samsung GT-S7580) the returned bitmap can be immutable, in that case
          // let's jut use Bitmap.createBitmap() to hopefully create a mutable one.
          return createFallbackBitmap(width, height, bitmapConfig);
        }
        bitmapRef.get().setHasAlpha(true);
        bitmapRef.get().eraseColor(Color.TRANSPARENT);
        return bitmapRef;
      } finally {
        EncodedImage.closeSafely(encodedImage);
      }
    } finally {
      jpgRef.close();
    }
  }

  private static CloseableReference<Bitmap> createFallbackBitmap(
      int width, int height, Bitmap.Config bitmapConfig) {
    return CloseableReference.of(
        Bitmap.createBitmap(width, height, bitmapConfig), SimpleBitmapReleaser.getInstance());
  }
}

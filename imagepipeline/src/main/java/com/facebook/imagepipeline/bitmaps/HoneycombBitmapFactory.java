/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.bitmaps;

import javax.annotation.concurrent.ThreadSafe;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;

import com.facebook.common.references.CloseableReference;
import com.facebook.imageformat.ImageFormat;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.platform.PlatformDecoder;

/**
 * Factory implementation for Honeycomb through Kitkat
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
@ThreadSafe
public class HoneycombBitmapFactory extends PlatformBitmapFactory {

  private final EmptyJpegGenerator mJpegGenerator;
  private final PlatformDecoder mPurgeableDecoder;

  public HoneycombBitmapFactory(EmptyJpegGenerator jpegGenerator,
                         PlatformDecoder purgeableDecoder) {
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
  @Override
  public CloseableReference<Bitmap> createBitmap(
      int width,
      int height,
      Bitmap.Config bitmapConfig) {
    CloseableReference<PooledByteBuffer> jpgRef = mJpegGenerator.generate(
        (short) width,
        (short) height);
    try {
      EncodedImage encodedImage = new EncodedImage(jpgRef);
      encodedImage.setImageFormat(ImageFormat.JPEG);
      try {
        CloseableReference<Bitmap> bitmapRef = mPurgeableDecoder.decodeJPEGFromEncodedImage(
            encodedImage, bitmapConfig, jpgRef.get().size());
        bitmapRef.get().eraseColor(Color.TRANSPARENT);
        return bitmapRef;
      } finally {
        EncodedImage.closeSafely(encodedImage);
      }
    } finally {
      jpgRef.close();
    }
  }
}

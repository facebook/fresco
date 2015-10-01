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
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.references.CloseableReference;
import com.facebook.imageformat.ImageFormat;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.memory.FlexByteArrayPool;
import com.facebook.imagepipeline.memory.PooledByteBuffer;

/**
 * Factory implementation for KitKat
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
@ThreadSafe
public class KitKatBitmapFactory extends DalvikBitmapFactory {

  private final EmptyJpegGenerator mJpegGenerator;

  KitKatBitmapFactory(EmptyJpegGenerator jpegGenerator,
      FlexByteArrayPool flexByteArrayPool) {
    super(flexByteArrayPool);
    this.mJpegGenerator = jpegGenerator;
  }

  /**
   * Creates a bitmap of the specified width and height.
   *
   * @param width the width of the bitmap
   * @param height the height of the bitmap
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
        CloseableReference<Bitmap> bitmapRef =
            decodeJPEGFromEncodedImage(encodedImage, bitmapConfig, jpgRef.get().size());
        bitmapRef.get().eraseColor(Color.TRANSPARENT);
        return bitmapRef;
      } finally {
        EncodedImage.closeSafely(encodedImage);
      }
    } finally {
      jpgRef.close();
    }
  }

  @Override
  protected boolean isPinBitmapEnabled() {
    return true;
  }

  /**
   * Decodes a byteArray into a purgeable bitmap
   *
   * @param bytesRef the byte buffer that contains the encoded bytes
   * @return
   */
  @Override
  protected Bitmap decodeByteArrayAsPurgeable(
      CloseableReference<PooledByteBuffer> bytesRef,
      BitmapFactory.Options options) {
    final PooledByteBuffer pooledByteBuffer = bytesRef.get();
    final int length = pooledByteBuffer.size();
    final CloseableReference<byte[]> encodedBytesArrayRef = mFlexByteArrayPool.get(length);
    try {
      final byte[] encodedBytesArray = encodedBytesArrayRef.get();
      pooledByteBuffer.read(0, encodedBytesArray, 0, length);
      Bitmap bitmap = BitmapFactory.decodeByteArray(
          encodedBytesArray,
          0,
          length,
          options);
      return Preconditions.checkNotNull(bitmap, "BitmapFactory returned null");
    } finally {
      CloseableReference.closeSafely(encodedBytesArrayRef);
    }
  }

  /**
   * Decodes a byteArray containing jpeg encoded bytes into a purgeable bitmap
   *
   * <p> Adds a JFIF End-Of-Image marker if needed before decoding.
   *
   * @param bytesRef the byte buffer that contains the encoded bytes
   * @return
   */
  @Override
  protected Bitmap decodeJPEGByteArrayAsPurgeable(
      CloseableReference<PooledByteBuffer> bytesRef,
      int length,
      BitmapFactory.Options options) {
    byte[] suffix = endsWithEOI(bytesRef, length) ? null : EOI;
    final PooledByteBuffer pooledByteBuffer = bytesRef.get();
    Preconditions.checkArgument(length <= pooledByteBuffer.size());
    // allocate bigger array in case EOI needs to be added
    final CloseableReference<byte[]> encodedBytesArrayRef = mFlexByteArrayPool.get(length + 2);
    try {
      byte[] encodedBytesArray = encodedBytesArrayRef.get();
      pooledByteBuffer.read(0, encodedBytesArray, 0, length);
      if (suffix != null) {
        putEOI(encodedBytesArray, length);
        length += 2;
      }
      Bitmap bitmap = BitmapFactory.decodeByteArray(
          encodedBytesArray,
          0,
          length,
          options);
      return Preconditions.checkNotNull(bitmap, "BitmapFactory returned null");
    } finally {
      CloseableReference.closeSafely(encodedBytesArrayRef);
    }
  }

}

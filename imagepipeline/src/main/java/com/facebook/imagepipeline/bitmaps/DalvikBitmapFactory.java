/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.bitmaps;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.Throwables;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.memory.BitmapCounter;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.memory.FlexByteArrayPool;
import com.facebook.imagepipeline.nativecode.Bitmaps;
import com.facebook.imageutils.JfifUtil;

/**
 * Bitmap factory for Dalvik VM (Honeycomb to KitKat).
 */
abstract class DalvikBitmapFactory extends PlatformBitmapFactory {

  private final FlexByteArrayPool mFlexByteArrayPool;

  DalvikBitmapFactory(FlexByteArrayPool flexByteArrayPool) {
    mFlexByteArrayPool = flexByteArrayPool;
  }

  /**
   * Creates a bitmap from encoded bytes.
   *
   * @param encodedImage the encoded image with reference to the encoded bytes
   * @param bitmapConfig the {@link android.graphics.Bitmap.Config}
   * used to create the decoded Bitmap
   * @return the bitmap
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  @Override
  public CloseableReference<Bitmap> decodeFromEncodedImage(
      final EncodedImage encodedImage,
      Bitmap.Config bitmapConfig) {
    BitmapFactory.Options options = getBitmapFactoryOptions(
        encodedImage.getSampleSize(),
        bitmapConfig);
    CloseableReference<PooledByteBuffer> bytesRef = encodedImage.getByteBufferRef();
    Preconditions.checkNotNull(bytesRef);
    try {
      Bitmap bitmap = decodeByteArrayAsPurgeable(bytesRef, options);
      return pinBitmap(bitmap);
    } finally {
      CloseableReference.closeSafely(bytesRef);
    }
  }

  /**
   * Creates a bitmap from encoded JPEG bytes. Supports a partial JPEG image.
   *
   * @param encodedImage the encoded image with reference to the encoded bytes
   * @param length the number of encoded bytes in the buffer
   * @param bitmapConfig the {@link android.graphics.Bitmap.Config}
   * used to create the decoded Bitmap
   * @return the bitmap
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  @Override
  public CloseableReference<Bitmap> decodeJPEGFromEncodedImage(
      final EncodedImage encodedImage,
      Bitmap.Config bitmapConfig,
      int length) {
    BitmapFactory.Options options = getBitmapFactoryOptions(
        encodedImage.getSampleSize(),
        bitmapConfig);
    final CloseableReference<PooledByteBuffer> bytesRef = encodedImage.getByteBufferRef();
    Preconditions.checkNotNull(bytesRef);
    try {
      Bitmap bitmap = decodeJPEGByteArrayAsPurgeable(bytesRef, length, options);
      return pinBitmap(bitmap);
    } finally {
      CloseableReference.closeSafely(bytesRef);
    }
  }

  /**
   * Pins the bitmap
   */
  private CloseableReference<Bitmap> pinBitmap(Bitmap bitmap) {
    try {
      // Real decoding happens here - if the image was corrupted, this will throw an exception
      Bitmaps.pinBitmap(bitmap);
    } catch (Exception e) {
      bitmap.recycle();
      throw Throwables.propagate(e);
    }

    if (!mUnpooledBitmapsCounter.increase(bitmap)) {
      bitmap.recycle();
      throw new TooManyBitmapsException();
    }

    return CloseableReference.of(bitmap, mUnpooledBitmapsReleaser);
  }

  /**
   * Decodes a byteArray into a purgeable bitmap
   *
   * @param bytesRef the byte buffer that contains the encoded bytes
   * @param options the options passed to the BitmapFactory
   * @return
   */
  private Bitmap decodeByteArrayAsPurgeable(
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
   * @param options the options passed to the BitmapFactory
   * @return
   */
  private Bitmap decodeJPEGByteArrayAsPurgeable(
      CloseableReference<PooledByteBuffer> bytesRef,
      int length,
      BitmapFactory.Options options) {
    final PooledByteBuffer pooledByteBuffer = bytesRef.get();
    Preconditions.checkArgument(length <= pooledByteBuffer.size());
    // allocate bigger array in case EOI needs to be added
    final CloseableReference<byte[]> encodedBytesArrayRef = mFlexByteArrayPool.get(length + 2);
    try {
      byte[] encodedBytesArray = encodedBytesArrayRef.get();
      pooledByteBuffer.read(0, encodedBytesArray, 0, length);
      if (!endsWithEOI(encodedBytesArray, length)) {
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

  private static BitmapFactory.Options getBitmapFactoryOptions(
      int sampleSize,
      Bitmap.Config bitmapConfig) {
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inDither = true; // known to improve picture quality at low cost
    options.inPreferredConfig = bitmapConfig;
    // Decode the image into a 'purgeable' bitmap that lives on the ashmem heap
    options.inPurgeable = true;
    // Sample size should ONLY be different than 1 when downsampling is enabled in the pipeline
    options.inSampleSize = sampleSize;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      options.inMutable = true;  // no known perf difference; allows postprocessing to work
    }
    return options;
  }

  private static void putEOI(byte[] imageBytes, int offset) {
    // TODO 5884402: remove dependency on JfifUtil
    imageBytes[offset] = (byte) JfifUtil.MARKER_FIRST_BYTE;
    imageBytes[offset + 1] = (byte) JfifUtil.MARKER_EOI;
  }

  private static boolean endsWithEOI(final byte[] imageBytes, int length) {
    // TODO 5884402: remove dependency on JfifUtil
    return length >= 2 &&
        imageBytes[length - 2] == (byte) JfifUtil.MARKER_FIRST_BYTE &&
        imageBytes[length - 1] == (byte) JfifUtil.MARKER_EOI;
  }

}

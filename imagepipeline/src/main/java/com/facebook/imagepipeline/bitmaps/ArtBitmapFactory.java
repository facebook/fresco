/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.bitmaps;

import javax.annotation.concurrent.GuardedBy;

import java.io.IOException;
import java.io.InputStream;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;

import com.facebook.common.references.CloseableReference;
import com.facebook.common.streams.LimitedInputStream;
import com.facebook.common.streams.TailAppendingInputStream;
import com.facebook.imagepipeline.memory.BitmapPool;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.memory.PooledByteBufferInputStream;
import com.facebook.imagepipeline.nativecode.Bitmaps;
import com.facebook.imageutils.JfifUtil;

/**
 * Bitmap factory for ART VM (Lollipop and up).
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class ArtBitmapFactory {

  /**
   * Size of temporary array. Value recommended by Android docs for decoding Bitmaps.
   */
  private static final int DECODE_BUFFER_SIZE = 16 * 1024;

  private final BitmapPool mBitmapPool;

  /**
   * ArtPlatformImageDecoder decodes images from InputStream - to do so we need to provide
   * temporary buffer, otherwise framework will allocate one for us for each decode request
   */
  @GuardedBy("this")
  private final byte[] mDecodeBuffer = new byte[DECODE_BUFFER_SIZE];


  // TODO (5884402) - remove dependency on JfifUtil
  private static final byte[] EOI_TAIL = new byte[] {
      (byte) JfifUtil.MARKER_FIRST_BYTE,
      (byte) JfifUtil.MARKER_EOI};

  public ArtBitmapFactory(BitmapPool bitmapPool) {
    mBitmapPool = bitmapPool;
  }

  /**
   * Creates a bitmap of the specified width and height.
   *
   * @param width the width of the bitmap
   * @param height the height of the bitmap
   * @return a reference to the bitmap
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  CloseableReference<Bitmap> createBitmap(int width, int height) {
    Bitmap bitmap = mBitmapPool.get(width * height);
    Bitmaps.reconfigureBitmap(bitmap, width, height);
    return CloseableReference.of(bitmap, mBitmapPool);
  }

  /**
   * Creates a bitmap from encoded bytes.
   *
   * @param pooledByteBufferRef the reference to the encoded bytes
   * @return the bitmap
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  CloseableReference<Bitmap> decodeFromPooledByteBuffer(
      CloseableReference<PooledByteBuffer> pooledByteBufferRef) {
    return doDecodeStaticImage(new PooledByteBufferInputStream(pooledByteBufferRef.get()));
  }

  /**
   * Creates a bitmap from encoded JPEG bytes. Supports a partial JPEG image.
   *
   * @param pooledByteBufferRef the reference to the encoded bytes
   * @param length the number of encoded bytes in the buffer
   * @return the bitmap
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  CloseableReference<Bitmap> decodeJPEGFromPooledByteBuffer(
      CloseableReference<PooledByteBuffer> pooledByteBufferRef,
      int length) {
    final PooledByteBuffer pooledByteBuffer = pooledByteBufferRef.get();
    final InputStream jpegBufferInputStream = new PooledByteBufferInputStream(pooledByteBuffer);
    jpegBufferInputStream.mark(Integer.MAX_VALUE);

    boolean isJpegComplete;
    try {
      jpegBufferInputStream.skip(length - 2);
      isJpegComplete = (jpegBufferInputStream.read() == JfifUtil.MARKER_FIRST_BYTE) &&
          (jpegBufferInputStream.read() == JfifUtil.MARKER_EOI);
      jpegBufferInputStream.reset();
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }

    InputStream jpegDataStream = jpegBufferInputStream;
    if (pooledByteBuffer.size() > length) {
      jpegDataStream = new LimitedInputStream(jpegDataStream, length);
    }
    if (!isJpegComplete) {
      jpegDataStream = new TailAppendingInputStream(jpegDataStream, EOI_TAIL);
    }
    return doDecodeStaticImage(jpegDataStream);
  }

  private CloseableReference<Bitmap> doDecodeStaticImage(InputStream inputStream) {
    inputStream.mark(Integer.MAX_VALUE);
    final BitmapFactory.Options options = getDecodeOptionsForStream(inputStream);
    try {
      inputStream.reset();
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }

    final Bitmap bitmapToReuse = mBitmapPool.get(options.outHeight * options.outWidth);
    if (bitmapToReuse == null) {
      throw new NullPointerException("BitmapPool.get returned null");
    }
    options.inBitmap = bitmapToReuse;

    Bitmap decodedBitmap;
    try {
      decodedBitmap = BitmapFactory.decodeStream(inputStream, null, options);
    } catch (RuntimeException re) {
      mBitmapPool.release(bitmapToReuse);
      throw re;
    }

    if (bitmapToReuse != decodedBitmap) {
      mBitmapPool.release(bitmapToReuse);
      decodedBitmap.recycle();
      throw new IllegalStateException();
    }

    return CloseableReference.of(decodedBitmap, mBitmapPool);
  }

  /**
   * Options returned by this method are configured with mDecodeBuffer which is GuardedBy("this")
   */
  private BitmapFactory.Options getDecodeOptionsForStream(InputStream inputStream) {
    final BitmapFactory.Options options = new BitmapFactory.Options();
    options.inTempStorage = mDecodeBuffer;

    options.inJustDecodeBounds = true;
    // fill outWidth and outHeight
    BitmapFactory.decodeStream(inputStream, null, options);
    if (options.outWidth == -1 || options.outHeight == -1) {
      throw new IllegalArgumentException();
    }

    options.inJustDecodeBounds = false;
    options.inDither = true;
    options.inPreferredConfig = Bitmaps.BITMAP_CONFIG;
    options.inMutable = true;

    return options;
  }
}

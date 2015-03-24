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
import java.util.List;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;

import com.facebook.common.internal.ByteStreams;
import com.facebook.common.internal.Lists;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.Throwables;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.references.ResourceReleaser;
import com.facebook.imagepipeline.memory.BitmapCounter;
import com.facebook.imagepipeline.memory.BitmapCounterProvider;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.memory.SingleByteArrayPool;
import com.facebook.imagepipeline.nativecode.Bitmaps;
import com.facebook.imageutils.JfifUtil;

/**
 * Bitmap factory for Dalvik VM (Honeycomb to KitKat).
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class DalvikBitmapFactory {

  private final EmptyJpegGenerator mJpegGenerator;
  private final BitmapCounter mUnpooledBitmapsCounter;
  private final ResourceReleaser<Bitmap> mUnpooledBitmapsReleaser;

  @GuardedBy("this")
  private final SingleByteArrayPool mSingleByteArrayPool;

  public DalvikBitmapFactory(
      EmptyJpegGenerator jpegGenerator,
      SingleByteArrayPool singleByteArrayPool) {
    mJpegGenerator = jpegGenerator;
    mSingleByteArrayPool = singleByteArrayPool;
    mUnpooledBitmapsCounter = BitmapCounterProvider.get();
    mUnpooledBitmapsReleaser = new ResourceReleaser<Bitmap>() {
      @Override
      public void release(Bitmap value) {
        try {
          mUnpooledBitmapsCounter.decrease(value);
        } finally {
          value.recycle();
        }
      }
    };
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
  CloseableReference<Bitmap> createBitmap(short width, short height) {
    CloseableReference<PooledByteBuffer> jpgRef = mJpegGenerator.generate(width, height);
    try {
      return decodeJPEGFromPooledByteBuffer(jpgRef, jpgRef.get().size());
    } finally {
      jpgRef.close();
    }
  }

  /**
   * Creates a bitmap from encoded bytes.
   *
   * @param pooledByteBufferRef the reference to the encoded bytes
   * @return the bitmap
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  synchronized CloseableReference<Bitmap> decodeFromPooledByteBuffer(
      final CloseableReference<PooledByteBuffer> pooledByteBufferRef) {
    final PooledByteBuffer pooledByteBuffer = pooledByteBufferRef.get();
    final int length = pooledByteBuffer.size();

    final byte[] encodedBytesArray = mSingleByteArrayPool.get(length);
    try {
      ByteStreams.readFully(pooledByteBuffer.getStream(), encodedBytesArray, 0, length);
      return doDecodeBitmap(encodedBytesArray, length);
    } catch (IOException ioe) {
      // does not happen, inputStream returned from pooledByteBuffer.getStream does not throw
      // IOExceptions
      throw Throwables.propagate(ioe);
    } finally {
      mSingleByteArrayPool.release(encodedBytesArray);
    }
  }

  /**
   * Creates a bitmap from encoded JPEG bytes. Supports a partial JPEG image.
   *
   * @param pooledByteBufferRef the reference to the encoded bytes
   * @param length the number of encoded bytes in the buffer
   * @return the bitmap
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  synchronized CloseableReference<Bitmap> decodeJPEGFromPooledByteBuffer(
      final CloseableReference<PooledByteBuffer> pooledByteBufferRef,
      int length) {
    final PooledByteBuffer pooledByteBuffer = pooledByteBufferRef.get();
    Preconditions.checkArgument(length <= pooledByteBuffer.size());
    // allocate bigger array in case EOI needs to be added
    final byte[] encodedBytesArray = mSingleByteArrayPool.get(length + 2);
    try {
      ByteStreams.readFully(pooledByteBuffer.getStream(), encodedBytesArray, 0, length);
      if (!endsWithEOI(encodedBytesArray, length)) {
        putEOI(encodedBytesArray, length);
        length += 2;
      }
      return doDecodeBitmap(
          encodedBytesArray,
          length);
    } catch (IOException ioe) {
      // does not happen, streams returned by pooledByteBuffer do not throw IOExceptions
      throw Throwables.propagate(ioe);
    } finally {
      mSingleByteArrayPool.release(encodedBytesArray);
    }
  }

  /**
   * Performs the actual decoding.
   */
  private CloseableReference<Bitmap> doDecodeBitmap(
      final byte[] encodedBytes,
      final int length) {
    final Bitmap bitmap = decodeAsPurgeableBitmap(encodedBytes, length);

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
   * Decode the input into a purgeable bitmap
   *
   * @param encodedBytes the input encoded image
   * @return a purgeable bitmap
   */
  @SuppressLint("NewApi")
  private static synchronized Bitmap decodeAsPurgeableBitmap(byte[] encodedBytes, int size) {
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inDither = true; // known to improve picture quality at low cost
    options.inPreferredConfig = Bitmaps.BITMAP_CONFIG;
    // Decode the image into a 'purgeable' bitmap that lives on the ashmem heap
    options.inPurgeable = true;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      options.inMutable = true;  // no known perf difference; allows postprocessing to work
    }
    Bitmap bitmap = BitmapFactory.decodeByteArray(
        encodedBytes,
        0,
        size,
        options);
    return Preconditions.checkNotNull(bitmap, "BitmapFactory returned null");
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

  /**
   * Associates bitmaps with the current bitmap pool. If this method throws
   * BitmapPoolSizeExceededException, the code will have called {@link Bitmap#recycle} on the
   * bitmaps.
   *
   * @param bitmaps the bitmaps to associate
   * @return the references to the bitmaps that are now tied to the bitmap pool
   * @throws TooManyBitmapsException if the pool is full
   */
  synchronized List<CloseableReference<Bitmap>> associateBitmapsWithBitmapCounter(
      final List<Bitmap> bitmaps) {
    int countedBitmaps = 0;
    try {
      for (; countedBitmaps < bitmaps.size(); ++countedBitmaps) {
        final Bitmap bitmap = bitmaps.get(countedBitmaps);
        // 'Pin' the bytes of the purgeable bitmap, so it is now not purgeable
        Bitmaps.pinBitmap(bitmap);
        if (!mUnpooledBitmapsCounter.increase(bitmap)) {
          throw new TooManyBitmapsException();
        }
      }
      List<CloseableReference<Bitmap>> ret = Lists.newArrayList();
      for (Bitmap bitmap : bitmaps) {
        ret.add(CloseableReference.of(bitmap, mUnpooledBitmapsReleaser));
      }
      return ret;
    } catch (Exception exception) {
      if (bitmaps != null) {
        for (Bitmap bitmap : bitmaps) {
          if (countedBitmaps-- > 0) {
            mUnpooledBitmapsCounter.decrease(bitmap);
          }
          bitmap.recycle();
        }
      }
      throw Throwables.propagate(exception);
    }
  }
}

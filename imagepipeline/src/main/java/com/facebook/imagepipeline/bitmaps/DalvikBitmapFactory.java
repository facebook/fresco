/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.bitmaps;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;

import com.facebook.common.internal.Closeables;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.Throwables;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.references.ResourceReleaser;
import com.facebook.imageformat.ImageFormat;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.memory.BitmapCounter;
import com.facebook.imagepipeline.memory.BitmapCounterProvider;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.memory.FlexByteArrayPool;
import com.facebook.imagepipeline.nativecode.Bitmaps;
import com.facebook.imageutils.JfifUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Bitmap factory for Dalvik VM (Honeycomb to KitKat).
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class DalvikBitmapFactory {

  private final EmptyJpegGenerator mJpegGenerator;
  private final BitmapCounter mUnpooledBitmapsCounter;
  private final ResourceReleaser<Bitmap> mUnpooledBitmapsReleaser;
  private final FlexByteArrayPool mFlexByteArrayPool;
  private final boolean mDownsampleEnabled;

  public DalvikBitmapFactory(
      EmptyJpegGenerator jpegGenerator,
      FlexByteArrayPool flexByteArrayPool,
      boolean downsampleEnabled) {
    mJpegGenerator = jpegGenerator;
    mFlexByteArrayPool = flexByteArrayPool;
    mDownsampleEnabled = downsampleEnabled;

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
      EncodedImage encodedImage = new EncodedImage(jpgRef);
      encodedImage.setImageFormat(ImageFormat.JPEG);
      try {
        CloseableReference<Bitmap> bitmapRef =
            decodeJPEGFromEncodedImage(encodedImage, jpgRef.get().size());
        bitmapRef.get().eraseColor(Color.TRANSPARENT);
        return bitmapRef;
      } finally {
        EncodedImage.closeSafely(encodedImage);
      }
    } finally {
      jpgRef.close();
    }
  }

  /**
   * Creates a bitmap from encoded bytes.
   *
   * @param encodedImage the encoded image with reference to the encoded bytes
   * @return the bitmap
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  CloseableReference<Bitmap> decodeFromEncodedImage(final EncodedImage encodedImage) {
    BitmapFactory.Options options = getBitmapFactoryOptions(
        encodedImage.getSampleSize(),
        mDownsampleEnabled);
    CloseableReference<PooledByteBuffer> bytesRef = encodedImage.getByteBufferRef();
    Bitmap bitmap;
    if (bytesRef != null) {
      try {
        bitmap = decodeByteArrayAsPurgeable(bytesRef, options);
      } finally {
        CloseableReference.closeSafely(bytesRef);
      }
    } else {
      bitmap = decodeEncodedImageWithInputStreamAsPurgeable(encodedImage, options);
    }
    return pinBitmap(bitmap);
  }

  /**
   * Creates a bitmap from encoded JPEG bytes. Supports a partial JPEG image.
   *
   * @param encodedImage the encoded image with reference to the encoded bytes
   * @param length the number of encoded bytes in the buffer
   * @return the bitmap
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  CloseableReference<Bitmap> decodeJPEGFromEncodedImage(
      final EncodedImage encodedImage,
      int length) {
    BitmapFactory.Options options = getBitmapFactoryOptions(
        encodedImage.getSampleSize(),
        mDownsampleEnabled);
    final CloseableReference<PooledByteBuffer> bytesRef = encodedImage.getByteBufferRef();
    Bitmap bitmap;
    if (bytesRef != null) {
      try {
        bitmap = decodeJPEGByteArrayAsPurgeable(bytesRef, length, options);
      } finally {
        CloseableReference.closeSafely(bytesRef);
      }
    } else {
      bitmap = decodeEncodedImageWithInputStreamAsPurgeable(encodedImage, options);
    }
    return pinBitmap(bitmap);
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

  /**
   * Gets the FileInputStream containing the encoded image and returns a purgeable bitmap from the
   * encoded bytes.
   *
   * <p> Only valid if the EncodedImage is backed by a supplier of FileInputStream.
   */
  private Bitmap decodeEncodedImageWithInputStreamAsPurgeable(
      EncodedImage encodedImage,
      BitmapFactory.Options options) {
    InputStream is = encodedImage.getInputStream();
    try {
      Preconditions.checkNotNull(is);
      Preconditions.checkArgument(is instanceof FileInputStream);
      return decodeFileInputStreamAsPurgeable((FileInputStream) is, options);
    } finally {
      Closeables.closeQuietly(is);
    }
  }

  /**
   * Decode the input into a purgeable bitmap
   *
   * @param fis the FileInputStream that contains the encoded bytes
   * @return a purgeable bitmap
   */
  private Bitmap decodeFileInputStreamAsPurgeable(
      FileInputStream fis,
      BitmapFactory.Options options) {
    Bitmap bitmap;
    try {
      bitmap = BitmapFactory.decodeFileDescriptor(
          fis.getFD(),
          null,
          options);
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
    return Preconditions.checkNotNull(bitmap, "BitmapFactory returned null");
  }

  private static BitmapFactory.Options getBitmapFactoryOptions(
      int sampleSize,
      boolean downsampleEnabled) {
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inDither = true; // known to improve picture quality at low cost
    options.inPreferredConfig = Bitmaps.BITMAP_CONFIG;
    // Decode the image into a 'purgeable' bitmap that lives on the ashmem heap
    options.inPurgeable = true;
    if (downsampleEnabled) {
      options.inSampleSize = sampleSize;
    }
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

  /**
   * Associates bitmaps with the current bitmap pool.
   *
   * <p> If this method throws TooManyBitmapsException, the code will have called
   * {@link Bitmap#recycle} on the bitmaps.
   *
   * @param bitmaps the bitmaps to associate
   * @return the references to the bitmaps that are now tied to the bitmap pool
   * @throws TooManyBitmapsException if the pool is full
   */
  List<CloseableReference<Bitmap>> associateBitmapsWithBitmapCounter(
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
      List<CloseableReference<Bitmap>> ret = new ArrayList<>();
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

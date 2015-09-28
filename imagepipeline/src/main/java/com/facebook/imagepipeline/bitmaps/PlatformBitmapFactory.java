/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.bitmaps;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.os.Build;

import com.facebook.common.internal.Throwables;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.references.ResourceReleaser;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.memory.BitmapCounter;
import com.facebook.imagepipeline.memory.BitmapCounterProvider;
import com.facebook.imagepipeline.memory.PoolFactory;
import com.facebook.imagepipeline.nativecode.Bitmaps;

/**
 * Bitmap factory optimized for the platform.
 */
public abstract class PlatformBitmapFactory {

  private static GingerbreadBitmapFactory sGingerbreadBitmapFactory;
  private static ArtBitmapFactory sArtBitmapFactory;
  private static HoneycombBitmapFactory sHoneycombBitmapFactory;

  protected final BitmapCounter mUnpooledBitmapsCounter;
  protected final ResourceReleaser<Bitmap> mUnpooledBitmapsReleaser;

  PlatformBitmapFactory() {
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
   * Provide the implementation of the PlatformBitmapFactory for the current platform using the
   * provided PoolFactory
   *
   * @param poolFactory The PoolFactory
   * @return The PlatformBitmapFactory implementation
   */
  public synchronized static PlatformBitmapFactory getInstance(
      final PoolFactory poolFactory) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      if (sArtBitmapFactory == null) {
        sArtBitmapFactory = new ArtBitmapFactory(
            poolFactory.getBitmapPool(),
            poolFactory.getFlexByteArrayPoolMaxNumThreads());
      }
      return sArtBitmapFactory;
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      if (sHoneycombBitmapFactory == null) {
        sHoneycombBitmapFactory = new HoneycombBitmapFactory(
            new EmptyJpegGenerator(poolFactory.getPooledByteBufferFactory()),
            poolFactory.getFlexByteArrayPool());
      }
      return sHoneycombBitmapFactory;
    } else {
      if (sGingerbreadBitmapFactory == null) {
        sGingerbreadBitmapFactory = new GingerbreadBitmapFactory(
            poolFactory.getFlexByteArrayPool()
        );
      }
      return sGingerbreadBitmapFactory;
    }
  }

  /**
   * Creates a bitmap of the specified width and height.
   *
   * @param width the width of the bitmap
   * @param height the height of the bitmap
   * @param bitmapConfig the Bitmap.Config used to create the Bitmap
   * @return a reference to the bitmap
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  public abstract CloseableReference<Bitmap> createBitmap(
      int width,
      int height,
      Bitmap.Config bitmapConfig);


  /**
   * Creates a bitmap of the specified width and height.
   * The bitmap will be created with the default ARGB_8888 configuration
   *
   * @param width the width of the bitmap
   * @param height the height of the bitmap
   * @return a reference to the bitmap
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  public CloseableReference<Bitmap> createBitmap(int width, int height) {
    return createBitmap(width, height, Bitmap.Config.ARGB_8888);
  }

  /**
   * Associates bitmaps with the bitmap counter. <p/> <p>If this method throws
   * TooManyBitmapsException, the code will have called {@link Bitmap#recycle} on the bitmaps.</p>
   *
   * @param bitmaps the bitmaps to associate
   * @return the references to the bitmaps that are now tied to the bitmap pool
   * @throws TooManyBitmapsException if the pool is full
   */
  public List<CloseableReference<Bitmap>> associateBitmapsWithBitmapCounter(
      final List<Bitmap> bitmaps) {
    int countedBitmaps = 0;
    try {
      for (; countedBitmaps < bitmaps.size(); ++countedBitmaps) {
        final Bitmap bitmap = bitmaps.get(countedBitmaps);
        // 'Pin' the bytes of the purgeable bitmap, so it is now not purgeable
        if (isPinBitmapEnabled()) {
          Bitmaps.pinBitmap(bitmap);
        }
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

  /**
   * Creates a bitmap from encoded bytes. Supports JPEG but callers should use {@link
   * #decodeJPEGFromEncodedImage} for partial JPEGs.
   *
   * @param encodedImage the reference to the encoded image with the reference to the encoded bytes
   * @param bitmapConfig the {@link android.graphics.Bitmap.Config} used to create the decoded
   * Bitmap
   * @return the bitmap
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  public abstract CloseableReference<Bitmap> decodeFromEncodedImage(
      final EncodedImage encodedImage,
      Bitmap.Config bitmapConfig);

  /**
   * Creates a bitmap from encoded JPEG bytes. Supports a partial JPEG image.
   *
   * @param encodedImage the reference to the encoded image with the reference to the encoded bytes
   * @param bitmapConfig the {@link android.graphics.Bitmap.Config} used to create the decoded
   * Bitmap
   * @param length the number of encoded bytes in the buffer
   * @return the bitmap
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  public abstract CloseableReference<Bitmap> decodeJPEGFromEncodedImage(
      EncodedImage encodedImage,
      Bitmap.Config bitmapConfig,
      int length);

  /**
   * We shoukd override this operation in case we want enable the pin for the Bitmap.
   *
   * @return True if the pin is enabled for Bitmap.
   */
  protected abstract boolean isPinBitmapEnabled();

}

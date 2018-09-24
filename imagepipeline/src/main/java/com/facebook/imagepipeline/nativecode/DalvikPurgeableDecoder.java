/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.nativecode;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Build;
import com.facebook.common.internal.DoNotStrip;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.Throwables;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.common.TooManyBitmapsException;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.memory.BitmapCounter;
import com.facebook.imagepipeline.memory.BitmapCounterProvider;
import com.facebook.imagepipeline.platform.PlatformDecoder;
import com.facebook.imageutils.BitmapUtil;
import com.facebook.imageutils.JfifUtil;
import java.util.Locale;
import javax.annotation.Nullable;

/**
 * Base class for bitmap decodes for Dalvik VM (Gingerbread to KitKat).
 *
 * <p>Native code used by this class is shipped as part of libimagepipeline.so
 */
@DoNotStrip
public abstract class DalvikPurgeableDecoder implements PlatformDecoder {

  static {
    ImagePipelineNativeLoader.load();
  }

  protected static final byte[] EOI = new byte[] {
      (byte) JfifUtil.MARKER_FIRST_BYTE, (byte) JfifUtil.MARKER_EOI };

  private final BitmapCounter mUnpooledBitmapsCounter;

  protected DalvikPurgeableDecoder() {
    mUnpooledBitmapsCounter = BitmapCounterProvider.get();
  }

  @Override
  public CloseableReference<Bitmap> decodeFromEncodedImage(
      EncodedImage encodedImage, Bitmap.Config bitmapConfig, @Nullable Rect regionToDecode) {
    return decodeFromEncodedImageWithColorSpace(encodedImage, bitmapConfig, regionToDecode, false);
  }

  @Override
  public CloseableReference<Bitmap> decodeJPEGFromEncodedImage(
      EncodedImage encodedImage,
      Bitmap.Config bitmapConfig,
      @Nullable Rect regionToDecode,
      int length) {
    return decodeJPEGFromEncodedImageWithColorSpace(
        encodedImage, bitmapConfig, regionToDecode, length, false);
  }

  /**
   * Creates a bitmap from encoded bytes.
   *
   * @param encodedImage the encoded image with reference to the encoded bytes
   * @param bitmapConfig the {@link android.graphics.Bitmap.Config} used to create the decoded
   *     Bitmap
   * @param regionToDecode optional image region to decode. currently not supported.
   * @param transformToSRGB whether to allow color space transformation to sRGB at load time
   * @return the bitmap
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  @Override
  public CloseableReference<Bitmap> decodeFromEncodedImageWithColorSpace(
      final EncodedImage encodedImage,
      Bitmap.Config bitmapConfig,
      @Nullable Rect regionToDecode,
      final boolean transformToSRGB) {
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
   * @param bitmapConfig the {@link android.graphics.Bitmap.Config} used to create the decoded
   *     Bitmap
   * @param regionToDecode optional image region to decode. currently not supported.
   * @param length the number of encoded bytes in the buffer
   * @param transformToSRGB whether to allow color space transformation to sRGB at load time
   * @return the bitmap
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  @Override
  public CloseableReference<Bitmap> decodeJPEGFromEncodedImageWithColorSpace(
      final EncodedImage encodedImage,
      Bitmap.Config bitmapConfig,
      @Nullable Rect regionToDecode,
      int length,
      final boolean transformToSRGB) {
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
   * Decodes a byteArray into a purgeable bitmap
   *
   * @param bytesRef the byte buffer that contains the encoded bytes
   * @param options the options passed to the BitmapFactory
   * @return
   */
  protected abstract Bitmap decodeByteArrayAsPurgeable(
      CloseableReference<PooledByteBuffer> bytesRef, BitmapFactory.Options options);

  /**
   * Decodes a byteArray containing jpeg encoded bytes into a purgeable bitmap
   *
   * <p>Adds a JFIF End-Of-Image marker if needed before decoding.
   *
   * @param bytesRef the byte buffer that contains the encoded bytes
   * @param length the number of encoded bytes in the buffer
   * @param options the options passed to the BitmapFactory
   * @return
   */
  protected abstract Bitmap decodeJPEGByteArrayAsPurgeable(
      CloseableReference<PooledByteBuffer> bytesRef, int length, BitmapFactory.Options options);

  @VisibleForTesting
  public static BitmapFactory.Options getBitmapFactoryOptions(
      int sampleSize, Bitmap.Config bitmapConfig) {
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inDither = true; // known to improve picture quality at low cost
    options.inPreferredConfig = bitmapConfig;
    // Decode the image into a 'purgeable' bitmap that lives on the ashmem heap
    options.inPurgeable = true;
    // Enable copy of of bitmap to enable purgeable decoding by filedescriptor
    options.inInputShareable = true;
    // Sample size should ONLY be different than 1 when downsampling is enabled in the pipeline
    options.inSampleSize = sampleSize;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      options.inMutable = true;  // no known perf difference; allows postprocessing to work
    }
    return options;
  }

  @VisibleForTesting
  public static boolean endsWithEOI(CloseableReference<PooledByteBuffer> bytesRef, int length) {
    PooledByteBuffer buffer = bytesRef.get();
    return length >= 2 &&
        buffer.read(length - 2) == (byte) JfifUtil.MARKER_FIRST_BYTE &&
        buffer.read(length - 1) == (byte) JfifUtil.MARKER_EOI;
  }

  /**
   * Pin the bitmap so that it cannot be 'purged'. Only makes sense for purgeable bitmaps WARNING:
   * Use with caution. Make sure that the pinned bitmap is recycled eventually. Otherwise, this will
   * simply eat up ashmem memory and eventually lead to unfortunate crashes. We *may* eventually
   * provide an unpin method - but we don't yet have a compelling use case for that.
   *
   * @param bitmap the purgeable bitmap to pin
   */
  public CloseableReference<Bitmap> pinBitmap(Bitmap bitmap) {
    Preconditions.checkNotNull(bitmap);
    try {
      // Real decoding happens here - if the image was corrupted, this will throw an exception
      nativePinBitmap(bitmap);
    } catch (Exception e) {
      bitmap.recycle();
      throw Throwables.propagate(e);
    }
    if (!mUnpooledBitmapsCounter.increase(bitmap)) {
      int bitmapSize = BitmapUtil.getSizeInBytes(bitmap);
      bitmap.recycle();
      String detailMessage = String.format(
          Locale.US,
          "Attempted to pin a bitmap of size %d bytes."
              + " The current pool count is %d, the current pool size is %d bytes."
              + " The current pool max count is %d, the current pool max size is %d bytes.",
          bitmapSize,
          mUnpooledBitmapsCounter.getCount(),
          mUnpooledBitmapsCounter.getSize(),
          mUnpooledBitmapsCounter.getMaxCount(),
          mUnpooledBitmapsCounter.getMaxSize());
      throw new TooManyBitmapsException(detailMessage);
    }
    return CloseableReference.of(bitmap, mUnpooledBitmapsCounter.getReleaser());
  }

  @DoNotStrip
  private static native void nativePinBitmap(Bitmap bitmap);
}

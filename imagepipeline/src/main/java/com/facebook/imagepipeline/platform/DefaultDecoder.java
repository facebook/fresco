/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.platform;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.ColorSpace;
import android.graphics.Rect;
import android.os.Build;
import androidx.core.util.Pools.SynchronizedPool;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.logging.FLog;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.streams.LimitedInputStream;
import com.facebook.common.streams.TailAppendingInputStream;
import com.facebook.imagepipeline.bitmaps.SimpleBitmapReleaser;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.memory.BitmapPool;
import com.facebook.imageutils.JfifUtil;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/** Bitmap decoder for ART VM (Lollipop and up). */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
@ThreadSafe
public abstract class DefaultDecoder implements PlatformDecoder {

  private static final Class<?> TAG = DefaultDecoder.class;

  /** Size of temporary array. Value recommended by Android docs for decoding Bitmaps. */
  private static final int DECODE_BUFFER_SIZE = 16 * 1024;

  private final BitmapPool mBitmapPool;

  private final @Nullable PreverificationHelper mPreverificationHelper;

  {
    mPreverificationHelper =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new PreverificationHelper() : null;
  }

  /**
   * ArtPlatformImageDecoder decodes images from InputStream - to do so we need to provide temporary
   * buffer, otherwise framework will allocate one for us for each decode request
   */
  @VisibleForTesting final SynchronizedPool<ByteBuffer> mDecodeBuffers;

  // TODO (5884402) - remove dependency on JfifUtil
  private static final byte[] EOI_TAIL =
      new byte[] {(byte) JfifUtil.MARKER_FIRST_BYTE, (byte) JfifUtil.MARKER_EOI};

  public DefaultDecoder(BitmapPool bitmapPool, int maxNumThreads, SynchronizedPool decodeBuffers) {
    mBitmapPool = bitmapPool;
    mDecodeBuffers = decodeBuffers;
    for (int i = 0; i < maxNumThreads; i++) {
      mDecodeBuffers.release(ByteBuffer.allocate(DECODE_BUFFER_SIZE));
    }
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
   * @param encodedImage the encoded image with a reference to the encoded bytes
   * @param bitmapConfig the {@link android.graphics.Bitmap.Config} used to create the decoded
   *     Bitmap
   * @param regionToDecode optional image region to decode or null to decode the whole image
   * @param transformToSRGB whether to allow color space transformation to sRGB at load time
   * @return the bitmap
   * @exception java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  @Override
  public CloseableReference<Bitmap> decodeFromEncodedImageWithColorSpace(
      EncodedImage encodedImage,
      Bitmap.Config bitmapConfig,
      @Nullable Rect regionToDecode,
      final boolean transformToSRGB) {
    final BitmapFactory.Options options = getDecodeOptionsForStream(encodedImage, bitmapConfig);
    boolean retryOnFail = options.inPreferredConfig != Bitmap.Config.ARGB_8888;
    try {
      return decodeFromStream(
          encodedImage.getInputStream(), options, regionToDecode, transformToSRGB);
    } catch (RuntimeException re) {
      if (retryOnFail) {
        return decodeFromEncodedImageWithColorSpace(
            encodedImage, Bitmap.Config.ARGB_8888, regionToDecode, transformToSRGB);
      }
      throw re;
    }
  }

  /**
   * Creates a bitmap from encoded JPEG bytes. Supports a partial JPEG image.
   *
   * @param encodedImage the encoded image with reference to the encoded bytes
   * @param bitmapConfig the {@link android.graphics.Bitmap.Config} used to create the decoded
   *     Bitmap
   * @param regionToDecode optional image region to decode or null to decode the whole image
   * @param length the number of encoded bytes in the buffer
   * @param transformToSRGB whether to allow color space transformation to sRGB at load time
   * @return the bitmap
   * @exception java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  @Override
  public CloseableReference<Bitmap> decodeJPEGFromEncodedImageWithColorSpace(
      EncodedImage encodedImage,
      Bitmap.Config bitmapConfig,
      @Nullable Rect regionToDecode,
      int length,
      final boolean transformToSRGB) {
    boolean isJpegComplete = encodedImage.isCompleteAt(length);
    final BitmapFactory.Options options = getDecodeOptionsForStream(encodedImage, bitmapConfig);
    InputStream jpegDataStream = encodedImage.getInputStream();
    // At this point the InputStream from the encoded image should not be null since in the
    // pipeline,this comes from a call stack where this was checked before. Also this method needs
    // the InputStream to decode the image so this can't be null.
    Preconditions.checkNotNull(jpegDataStream);
    if (encodedImage.getSize() > length) {
      jpegDataStream = new LimitedInputStream(jpegDataStream, length);
    }
    if (!isJpegComplete) {
      jpegDataStream = new TailAppendingInputStream(jpegDataStream, EOI_TAIL);
    }
    boolean retryOnFail = options.inPreferredConfig != Bitmap.Config.ARGB_8888;
    try {
      return decodeFromStream(jpegDataStream, options, regionToDecode, transformToSRGB);
    } catch (RuntimeException re) {
      if (retryOnFail) {
        return decodeJPEGFromEncodedImageWithColorSpace(
            encodedImage, Bitmap.Config.ARGB_8888, regionToDecode, length, transformToSRGB);
      }
      throw re;
    }
  }

  /**
   * This method is needed because of dependency issues.
   *
   * @param inputStream the InputStream
   * @param options the {@link android.graphics.BitmapFactory.Options} used to decode the stream
   * @param regionToDecode optional image region to decode or null to decode the whole image
   * @return the bitmap
   */
  protected CloseableReference<Bitmap> decodeStaticImageFromStream(
      InputStream inputStream, BitmapFactory.Options options, @Nullable Rect regionToDecode) {
    return decodeFromStream(inputStream, options, regionToDecode, false);
  }

  /**
   * Create a bitmap from an input stream.
   *
   * @param inputStream the InputStream
   * @param options the {@link android.graphics.BitmapFactory.Options} used to decode the stream
   * @param regionToDecode optional image region to decode or null to decode the whole image
   * @param transformToSRGB whether to allow color space transformation to sRGB at load time
   * @return the bitmap
   */
  private CloseableReference<Bitmap> decodeFromStream(
      InputStream inputStream,
      BitmapFactory.Options options,
      @Nullable Rect regionToDecode,
      final boolean transformToSRGB) {
    Preconditions.checkNotNull(inputStream);
    int targetWidth = options.outWidth;
    int targetHeight = options.outHeight;
    if (regionToDecode != null) {
      targetWidth = regionToDecode.width() / options.inSampleSize;
      targetHeight = regionToDecode.height() / options.inSampleSize;
    }
    @Nullable Bitmap bitmapToReuse = null;
    boolean shouldUseHardwareBitmapConfig = false;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      shouldUseHardwareBitmapConfig =
          mPreverificationHelper != null
              && mPreverificationHelper.shouldUseHardwareBitmapConfig(options.inPreferredConfig);
    }
    if (regionToDecode == null && shouldUseHardwareBitmapConfig) {
      // Cannot reuse bitmaps with Bitmap.Config.HARDWARE
      options.inMutable = false;
    } else {
      if (regionToDecode != null && shouldUseHardwareBitmapConfig) {
        // If region decoding was requested we need to fallback to default config
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
      }
      final int sizeInBytes = getBitmapSize(targetWidth, targetHeight, options);
      bitmapToReuse = mBitmapPool.get(sizeInBytes);
      if (bitmapToReuse == null) {
        throw new NullPointerException("BitmapPool.get returned null");
      }
    }
    // inBitmap can be nullable
    //noinspection ConstantConditions
    options.inBitmap = bitmapToReuse;

    // Performs transformation at load time to sRGB.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && transformToSRGB) {
      options.inPreferredColorSpace = ColorSpace.get(ColorSpace.Named.SRGB);
    }

    Bitmap decodedBitmap = null;
    ByteBuffer byteBuffer = mDecodeBuffers.acquire();
    if (byteBuffer == null) {
      byteBuffer = ByteBuffer.allocate(DECODE_BUFFER_SIZE);
    }
    try {
      options.inTempStorage = byteBuffer.array();
      if (regionToDecode != null && bitmapToReuse != null) {
        BitmapRegionDecoder bitmapRegionDecoder = null;
        try {
          bitmapToReuse.reconfigure(targetWidth, targetHeight, options.inPreferredConfig);
          bitmapRegionDecoder = BitmapRegionDecoder.newInstance(inputStream, true);
          decodedBitmap = bitmapRegionDecoder.decodeRegion(regionToDecode, options);
        } catch (IOException e) {
          FLog.e(TAG, "Could not decode region %s, decoding full bitmap instead.", regionToDecode);
        } finally {
          if (bitmapRegionDecoder != null) {
            bitmapRegionDecoder.recycle();
          }
        }
      }
      if (decodedBitmap == null) {
        decodedBitmap = BitmapFactory.decodeStream(inputStream, null, options);
      }
    } catch (IllegalArgumentException e) {
      if (bitmapToReuse != null) {
        mBitmapPool.release(bitmapToReuse);
      }
      // This is thrown if the Bitmap options are invalid, so let's just try to decode the bitmap
      // as-is, which might be inefficient - but it works.
      try {
        // We need to reset the stream first
        inputStream.reset();

        Bitmap naiveDecodedBitmap = BitmapFactory.decodeStream(inputStream);
        if (naiveDecodedBitmap == null) {
          throw e;
        }
        return CloseableReference.of(naiveDecodedBitmap, SimpleBitmapReleaser.getInstance());
      } catch (IOException re) {
        // We throw the original exception instead since it's the one causing this workaround in the
        // first place.
        throw e;
      }
    } catch (RuntimeException re) {
      if (bitmapToReuse != null) {
        mBitmapPool.release(bitmapToReuse);
      }
      throw re;
    } finally {
      mDecodeBuffers.release(byteBuffer);
    }

    // If bitmap with Bitmap.Config.HARDWARE was used, `bitmapToReuse` will be null and it's
    // expected
    if (bitmapToReuse != null && bitmapToReuse != decodedBitmap) {
      mBitmapPool.release(bitmapToReuse);
      decodedBitmap.recycle();
      throw new IllegalStateException();
    }

    return CloseableReference.of(decodedBitmap, mBitmapPool);
  }

  /**
   * Options returned by this method are configured with mDecodeBuffer which is GuardedBy("this")
   */
  private static BitmapFactory.Options getDecodeOptionsForStream(
      EncodedImage encodedImage, Bitmap.Config bitmapConfig) {
    final BitmapFactory.Options options = new BitmapFactory.Options();
    // Sample size should ONLY be different than 1 when downsampling is enabled in the pipeline
    options.inSampleSize = encodedImage.getSampleSize();
    options.inJustDecodeBounds = true;
    // fill outWidth and outHeight
    BitmapFactory.decodeStream(encodedImage.getInputStream(), null, options);
    if (options.outWidth == -1 || options.outHeight == -1) {
      throw new IllegalArgumentException();
    }

    options.inJustDecodeBounds = false;
    options.inDither = true;
    options.inPreferredConfig = bitmapConfig;
    options.inMutable = true;

    return options;
  }

  public abstract int getBitmapSize(
      final int width, final int height, final BitmapFactory.Options options);
}

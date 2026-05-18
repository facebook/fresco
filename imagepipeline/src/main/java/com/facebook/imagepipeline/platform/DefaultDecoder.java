/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.platform;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorSpace;
import android.graphics.Rect;
import androidx.core.util.Pools;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.references.ResourceReleaser;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.memory.BitmapPool;
import com.facebook.imagepipeline.memory.DummyBitmapPool;
import com.facebook.infer.annotation.Nullsafe;
import java.io.InputStream;
import java.nio.ByteBuffer;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/** Bitmap decoder for ART VM (Lollipop and up). */
@ThreadSafe
@Nullsafe(Nullsafe.Mode.LOCAL)
public abstract class DefaultDecoder implements PlatformDecoder {

  private final BitmapPool mBitmapPool;
  private final RawBitmapDecoder mRawBitmapDecoder;
  private boolean mAvoidPoolGet;
  private boolean mAvoidPoolRelease;

  public DefaultDecoder(
      BitmapPool bitmapPool,
      Pools.Pool<ByteBuffer> decodeBuffers,
      PlatformDecoderOptions platformDecoderOptions,
      RawBitmapDecoder rawBitmapDecoder) {
    mBitmapPool = bitmapPool;
    mRawBitmapDecoder = rawBitmapDecoder;
    if (bitmapPool instanceof DummyBitmapPool) {
      mAvoidPoolGet = platformDecoderOptions.getAvoidPoolGet();
      mAvoidPoolRelease = platformDecoderOptions.getAvoidPoolRelease();
    }
  }

  @Override
  public @Nullable CloseableReference<Bitmap> decodeFromEncodedImage(
      EncodedImage encodedImage, Bitmap.Config bitmapConfig, @Nullable Rect regionToDecode) {
    return decodeFromEncodedImageWithColorSpace(encodedImage, bitmapConfig, regionToDecode, null);
  }

  @Override
  public @Nullable CloseableReference<Bitmap> decodeJPEGFromEncodedImage(
      EncodedImage encodedImage,
      Bitmap.Config bitmapConfig,
      @Nullable Rect regionToDecode,
      int length) {
    return decodeJPEGFromEncodedImageWithColorSpace(
        encodedImage, bitmapConfig, regionToDecode, length, null);
  }

  /**
   * Creates a bitmap from encoded bytes.
   *
   * @param encodedImage the encoded image with a reference to the encoded bytes
   * @param bitmapConfig the {@link android.graphics.Bitmap.Config} used to create the decoded
   *     Bitmap
   * @param regionToDecode optional image region to decode or null to decode the whole image
   * @param colorSpace the target color space of the decoded bitmap, must be one of the named color
   *     space in {@link android.graphics.ColorSpace.Named}. If null, then SRGB color space is
   *     assumed if the SDK version >= 26.
   * @return the bitmap
   * @exception java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  @Override
  public @Nullable CloseableReference<Bitmap> decodeFromEncodedImageWithColorSpace(
      EncodedImage encodedImage,
      Bitmap.Config bitmapConfig,
      @Nullable Rect regionToDecode,
      @Nullable final ColorSpace colorSpace) {
    boolean retryOnFail = bitmapConfig != Bitmap.Config.ARGB_8888;
    try {
      InputStream stream = Preconditions.checkNotNull(encodedImage.getInputStream());
      Bitmap decodedBitmap =
          mRawBitmapDecoder.decode(
              stream,
              mAvoidPoolGet ? null : encodedImage.getInputStream(),
              encodedImage.getSampleSize(),
              bitmapConfig,
              regionToDecode,
              colorSpace);
      return wrapBitmap(decodedBitmap);
    } catch (RuntimeException re) {
      if (retryOnFail) {
        return decodeFromEncodedImageWithColorSpace(
            encodedImage, Bitmap.Config.ARGB_8888, regionToDecode, colorSpace);
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
   * @param colorSpace the target color space of the decoded bitmap, must be one of the named color
   *     space in {@link android.graphics.ColorSpace.Named}. If null, then SRGB color space is
   *     assumed if the SDK version >= 26.
   * @return the bitmap
   * @exception java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  @Override
  public @Nullable CloseableReference<Bitmap> decodeJPEGFromEncodedImageWithColorSpace(
      EncodedImage encodedImage,
      Bitmap.Config bitmapConfig,
      @Nullable Rect regionToDecode,
      int length,
      @Nullable final ColorSpace colorSpace) {
    boolean isJpegComplete = encodedImage.isCompleteAt(length);
    boolean retryOnFail = bitmapConfig != Bitmap.Config.ARGB_8888;
    InputStream jpegDataStream = encodedImage.getInputStream();
    Preconditions.checkNotNull(jpegDataStream);
    try {
      Bitmap decodedBitmap =
          mRawBitmapDecoder.decodeJpeg(
              jpegDataStream,
              mAvoidPoolGet ? null : encodedImage.getInputStream(),
              encodedImage.getSampleSize(),
              bitmapConfig,
              regionToDecode,
              colorSpace,
              encodedImage.getSize(),
              length,
              isJpegComplete);
      return wrapBitmap(decodedBitmap);
    } catch (RuntimeException re) {
      if (retryOnFail) {
        return decodeJPEGFromEncodedImageWithColorSpace(
            encodedImage, Bitmap.Config.ARGB_8888, regionToDecode, length, colorSpace);
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
  protected @Nullable CloseableReference<Bitmap> decodeStaticImageFromStream(
      InputStream inputStream, BitmapFactory.Options options, @Nullable Rect regionToDecode) {
    Bitmap decodedBitmap = mRawBitmapDecoder.decode(inputStream, options, regionToDecode, null);
    return wrapBitmap(decodedBitmap);
  }

  private @Nullable CloseableReference<Bitmap> wrapBitmap(@Nullable Bitmap bitmap) {
    if (bitmap == null) {
      return null;
    }
    if (mAvoidPoolRelease) {
      return CloseableReference.of(bitmap, NoOpResourceReleaser.INSTANCE);
    } else {
      return CloseableReference.of(bitmap, mBitmapPool);
    }
  }

  public abstract int getBitmapSize(
      final int width, final int height, final BitmapFactory.Options options);

  private static final class NoOpResourceReleaser implements ResourceReleaser<Bitmap> {
    private static final NoOpResourceReleaser INSTANCE = new NoOpResourceReleaser();

    @Override
    public void release(Bitmap value) {
      // NoOp
    }
  }
}

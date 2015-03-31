/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.decoder;

import javax.annotation.Nullable;

import android.graphics.Bitmap;

import com.facebook.common.references.CloseableReference;
import com.facebook.imageformat.ImageFormat;
import com.facebook.imageformat.ImageFormatChecker;
import com.facebook.imagepipeline.animated.factory.AnimatedImageFactory;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.CloseableStaticBitmap;
import com.facebook.imagepipeline.image.ImmutableQualityInfo;
import com.facebook.imagepipeline.image.QualityInfo;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.memory.PooledByteBufferInputStream;

/**
 * Decodes images.
 *
 * <p> ImageDecoder implements image type recognition and passes decode requests to
 * specialized methods implemented by subclasses.
 *
 * On dalvik, it produces 'pinned' purgeable bitmaps.
 *
 * <p> Pinned purgeables behave as specified in
 * {@link android.graphics.BitmapFactory.Options#inPurgeable} with one modification. The bitmap is
 * 'pinned' so is never purged.
 *
 * <p> For API 21 and higher, this class produces standard Bitmaps, as purgeability is not supported
 * on the most recent versions of Android.
 */
public class ImageDecoder {

  private final AnimatedImageFactory mAnimatedImageFactory;
  private final PlatformBitmapFactory mBitmapFactoryWithPool;

  public ImageDecoder(
      final AnimatedImageFactory animatedImageFactory,
      final PlatformBitmapFactory bitmapFactoryWithPool) {
    mAnimatedImageFactory = animatedImageFactory;
    mBitmapFactoryWithPool = bitmapFactoryWithPool;
  }

  /**
   * Decodes image.
   *
   * @param pooledByteBufferRef buffer containing image data
   * @param imageFormat if not null and not UNKNOWN, then format check is skipped and this one is
   *   assumed.
   * @param length if image type supports decoding incomplete image then determines where
   *   the image data should be cut for decoding.
   * @param qualityInfo quality information for the image
   * @param options options that cange decode behavior
   */
  public CloseableImage decodeImage(
      final CloseableReference<PooledByteBuffer> pooledByteBufferRef,
      @Nullable ImageFormat imageFormat,
      final int length,
      final QualityInfo qualityInfo,
      final ImageDecodeOptions options) {
    if (imageFormat == null || imageFormat == ImageFormat.UNKNOWN) {
      imageFormat = ImageFormatChecker.getImageFormat_WrapIOException(
          new PooledByteBufferInputStream(pooledByteBufferRef.get()));
    }

    switch (imageFormat) {
      case UNKNOWN:
        throw new IllegalArgumentException("unknown image format");

      case JPEG:
        return decodeJpeg(pooledByteBufferRef, length, qualityInfo);

      case GIF:
        return decodeAnimatedGif(pooledByteBufferRef, options);

      case WEBP_ANIMATED:
        return decodeAnimatedWebp(pooledByteBufferRef, options);

      default:
        return decodeStaticImage(pooledByteBufferRef);
    }
  }

  /**
   * Decodes gif into CloseableImage.
   *
   * @param pooledByteBufferRef
   * @return a CloseableGifImage
   */
  public CloseableImage decodeAnimatedGif(
      CloseableReference<PooledByteBuffer> pooledByteBufferRef,
      ImageDecodeOptions options) {
    return mAnimatedImageFactory.decodeGif(pooledByteBufferRef, options);
  }

  /**
   * @param pooledByteBufferRef input image (encoded bytes)
   * @return a CloseableStaticBitmap
   */
  public synchronized CloseableStaticBitmap decodeStaticImage(
      final CloseableReference<PooledByteBuffer> pooledByteBufferRef) {
    CloseableReference<Bitmap> bitmapReference =
        mBitmapFactoryWithPool.decodeFromPooledByteBuffer(pooledByteBufferRef);
    try {
      return new CloseableStaticBitmap(bitmapReference, ImmutableQualityInfo.FULL_QUALITY);
    } finally {
      bitmapReference.close();
    }
  }

  /**
   * Decodes a partial jpeg.
   *
   * @param pooledByteBufferRef
   * @param length amount of currently available data in bytes
   * @param qualityInfo quality info for the image
   * @return a CloseableStaticBitmap
   */
  public synchronized CloseableStaticBitmap decodeJpeg(
      final CloseableReference<PooledByteBuffer> pooledByteBufferRef,
      int length,
      QualityInfo qualityInfo) {
    CloseableReference<Bitmap> bitmapReference =
        mBitmapFactoryWithPool.decodeJPEGFromPooledByteBuffer(pooledByteBufferRef, length);
    try {
      return new CloseableStaticBitmap(bitmapReference, qualityInfo);
    } finally {
      bitmapReference.close();
    }
  }

  /**
   * Decode a webp animated image into a CloseableImage.
   *
   * <p> The image is decoded into a 'pinned' purgeable bitmap.
   *
   * @param pooledByteBufferRef input image (encoded bytes)
   * @param options
   * @return a {@link CloseableImage}
   */
  public CloseableImage decodeAnimatedWebp(
      final CloseableReference<PooledByteBuffer> pooledByteBufferRef,
      final ImageDecodeOptions options) {
    return mAnimatedImageFactory.decodeWebP(pooledByteBufferRef, options);
  }

}

/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.decoder;

import android.graphics.Bitmap;

import com.facebook.common.references.CloseableReference;
import com.facebook.imageformat.GifFormatChecker;
import com.facebook.imageformat.ImageFormat;
import com.facebook.imageformat.ImageFormatChecker;
import com.facebook.imagepipeline.animated.factory.AnimatedImageFactory;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.CloseableStaticBitmap;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.image.ImmutableQualityInfo;
import com.facebook.imagepipeline.image.QualityInfo;
import com.facebook.imagepipeline.memory.PooledByteBufferInputStream;

import java.io.InputStream;

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
   * @param encodedImage input image (encoded bytes plus meta data)
   * @param length if image type supports decoding incomplete image then determines where
   *   the image data should be cut for decoding.
   * @param qualityInfo quality information for the image
   * @param options options that cange decode behavior
   */
  public CloseableImage decodeImage(
      final EncodedImage encodedImage,
      final int length,
      final QualityInfo qualityInfo,
      final ImageDecodeOptions options) {
    ImageFormat imageFormat = encodedImage.getImageFormat();
    if (imageFormat == null || imageFormat == ImageFormat.UNKNOWN) {
      imageFormat = ImageFormatChecker.getImageFormat_WrapIOException(
          encodedImage.getInputStream());
    }

    switch (imageFormat) {
      case UNKNOWN:
        throw new IllegalArgumentException("unknown image format");

      case JPEG:
        return decodeJpeg(encodedImage, length, qualityInfo);

      case GIF:
        return decodeGif(encodedImage, options);

      case WEBP_ANIMATED:
        return decodeAnimatedWebp(encodedImage, options);

      default:
        return decodeStaticImage(encodedImage);
    }
  }

  /**
   * Decodes gif into CloseableImage.
   *
   * @param encodedImage input image (encoded bytes plus meta data)
   * @return a CloseableImage
   */
  public CloseableImage decodeGif(
      EncodedImage encodedImage,
      ImageDecodeOptions options) {
    InputStream is = encodedImage.getInputStream();
    if (is == null) {
      return null;
    }
    if (GifFormatChecker.isAnimated(is)) {
      return mAnimatedImageFactory.decodeGif(encodedImage, options);
    }
    return decodeStaticImage(encodedImage);
  }

  /**
   * @param encodedImage input image (encoded bytes plus meta data)
   * @return a CloseableStaticBitmap
   */
  public CloseableStaticBitmap decodeStaticImage(
      final EncodedImage encodedImage) {
    CloseableReference<Bitmap> bitmapReference =
        mBitmapFactoryWithPool.decodeFromEncodedImage(encodedImage);
    try {
      return new CloseableStaticBitmap(
          bitmapReference,
          ImmutableQualityInfo.FULL_QUALITY,
          encodedImage.getRotationAngle());
    } finally {
      bitmapReference.close();
    }
  }

  /**
   * Decodes a partial jpeg.
   *
   * @param encodedImage input image (encoded bytes plus meta data)
   * @param length amount of currently available data in bytes
   * @param qualityInfo quality info for the image
   * @return a CloseableStaticBitmap
   */
  public CloseableStaticBitmap decodeJpeg(
      final EncodedImage encodedImage,
      int length,
      QualityInfo qualityInfo) {
    CloseableReference<Bitmap> bitmapReference =
        mBitmapFactoryWithPool.decodeJPEGFromEncodedImage(encodedImage, length);
    try {
      return new CloseableStaticBitmap(
          bitmapReference,
          qualityInfo,
          encodedImage.getRotationAngle());
    } finally {
      bitmapReference.close();
    }
  }

  /**
   * Decode a webp animated image into a CloseableImage.
   *
   * <p> The image is decoded into a 'pinned' purgeable bitmap.
   *
   * @param encodedImage input image (encoded bytes plus meta data)
   * @param options
   * @return a {@link CloseableImage}
   */
  public CloseableImage decodeAnimatedWebp(
      final EncodedImage encodedImage,
      final ImageDecodeOptions options) {
    return mAnimatedImageFactory.decodeWebP(encodedImage, options);
  }

}

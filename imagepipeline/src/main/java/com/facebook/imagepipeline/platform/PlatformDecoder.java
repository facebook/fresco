/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.platform;

import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import android.graphics.Rect;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.image.EncodedImage;
import javax.annotation.Nullable;

public interface PlatformDecoder {
  /**
   * Creates a bitmap from encoded bytes. Supports JPEG but callers should use {@link
   * #decodeJPEGFromEncodedImage} for partial JPEGs. In addition, a region to decode can be supplied
   * in order to minimize memory usage. NOTE: Not all platform decoders necessarily support
   * supplying specific regions.
   *
   * <p>Note: This needs to be kept because of dependencies issues.
   *
   * @param encodedImage the reference to the encoded image with the reference to the encoded bytes
   * @param bitmapConfig the {@link android.graphics.Bitmap.Config} used to create the decoded
   *     Bitmap
   * @param regionToDecode optional image region to decode or null to decode the whole image
   * @return the bitmap
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  CloseableReference<Bitmap> decodeFromEncodedImage(
      final EncodedImage encodedImage, Bitmap.Config bitmapConfig, @Nullable Rect regionToDecode);

  /**
   * Creates a bitmap from encoded JPEG bytes. Supports a partial JPEG image. In addition, a region
   * to decode can be supplied in order to minimize memory usage. NOTE: Not all platform decoders
   * necessarily support supplying specific regions.
   *
   * <p>Note: This needs to be kept because of dependencies issues.
   *
   * @param encodedImage the reference to the encoded image with the reference to the encoded bytes
   * @param bitmapConfig the {@link android.graphics.Bitmap.Config} used to create the decoded
   *     Bitmap
   * @param regionToDecode optional image region to decode or null to decode the whole image.
   * @param length the number of encoded bytes in the buffer
   * @return the bitmap
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  CloseableReference<Bitmap> decodeJPEGFromEncodedImage(
      EncodedImage encodedImage,
      Bitmap.Config bitmapConfig,
      @Nullable Rect regionToDecode,
      int length);

  /**
   * Creates a bitmap from encoded bytes. Supports JPEG but callers should use {@link
   * #decodeJPEGFromEncodedImage} for partial JPEGs. In addition, a region to decode can be supplied
   * in order to minimize memory usage. NOTE: Not all platform decoders necessarily support
   * supplying specific regions.
   *
   * @param encodedImage the reference to the encoded image with the reference to the encoded bytes
   * @param bitmapConfig the {@link android.graphics.Bitmap.Config} used to create the decoded
   *     Bitmap
   * @param regionToDecode optional image region to decode or null to decode the whole image
   * @param colorSpace the target color space of the decoded bitmap, must be one of the named color
   *     space in {@link android.graphics.ColorSpace.Named}. If null, then SRGB color space is
   *     assumed if the SDK version >= 26.
   * @return the bitmap
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  CloseableReference<Bitmap> decodeFromEncodedImageWithColorSpace(
      final EncodedImage encodedImage,
      Bitmap.Config bitmapConfig,
      @Nullable Rect regionToDecode,
      @Nullable final ColorSpace colorSpace);

  /**
   * Creates a bitmap from encoded JPEG bytes. Supports a partial JPEG image. In addition, a region
   * to decode can be supplied in order to minimize memory usage. NOTE: Not all platform decoders
   * necessarily support supplying specific regions.
   *
   * @param encodedImage the reference to the encoded image with the reference to the encoded bytes
   * @param bitmapConfig the {@link android.graphics.Bitmap.Config} used to create the decoded
   *     Bitmap
   * @param regionToDecode optional image region to decode or null to decode the whole image.
   * @param length the number of encoded bytes in the buffer
   * @param colorSpace the target color space of the decoded bitmap, must be one of the named color
   *     space in {@link android.graphics.ColorSpace.Named}. If null, then SRGB color space is
   *     assumed if the SDK version >= 26.
   * @return the bitmap
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  CloseableReference<Bitmap> decodeJPEGFromEncodedImageWithColorSpace(
      EncodedImage encodedImage,
      Bitmap.Config bitmapConfig,
      @Nullable Rect regionToDecode,
      int length,
      @Nullable final ColorSpace colorSpace);
}

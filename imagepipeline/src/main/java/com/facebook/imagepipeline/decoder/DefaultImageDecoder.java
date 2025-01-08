/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.decoder;

import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.Supplier;
import com.facebook.common.internal.Suppliers;
import com.facebook.common.references.CloseableReference;
import com.facebook.fresco.middleware.HasExtraData;
import com.facebook.imageformat.DefaultImageFormats;
import com.facebook.imageformat.ImageFormat;
import com.facebook.imageformat.ImageFormatChecker;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.CloseableStaticBitmap;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.image.ImmutableQualityInfo;
import com.facebook.imagepipeline.image.QualityInfo;
import com.facebook.imagepipeline.platform.PlatformDecoder;
import com.facebook.imagepipeline.transformation.CircularTransformation;
import com.facebook.imagepipeline.transformation.TransformationUtils;
import com.facebook.infer.annotation.Nullsafe;
import java.io.InputStream;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Decodes images.
 *
 * <p>ImageDecoder implements image type recognition and passes decode requests to specialized
 * methods implemented by subclasses.
 *
 * <p>On dalvik, it produces 'pinned' purgeable bitmaps.
 *
 * <p>Pinned purgeables behave as specified in {@link
 * android.graphics.BitmapFactory.Options#inPurgeable} with one modification. The bitmap is 'pinned'
 * so is never purged.
 *
 * <p>For API 21 and higher, this class produces standard Bitmaps, as purgeability is not supported
 * on the most recent versions of Android.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class DefaultImageDecoder implements ImageDecoder {

  private final @Nullable ImageDecoder mAnimatedGifDecoder;
  private final @Nullable ImageDecoder mAnimatedWebPDecoder;
  private final @Nullable ImageDecoder mXmlDecoder;
  private final PlatformDecoder mPlatformDecoder;
  private final Supplier<Boolean> mEnableEncodedImageColorSpaceUsage;

  private final ImageDecoder mDefaultDecoder =
      new ImageDecoder() {
        @Override
        public @Nullable CloseableImage decode(
            EncodedImage encodedImage,
            int length,
            QualityInfo qualityInfo,
            ImageDecodeOptions options) {
          ImageFormat imageFormat = encodedImage.getImageFormat();
          ColorSpace colorSpace = null;
          if (mEnableEncodedImageColorSpaceUsage.get()) {
            colorSpace =
                options.colorSpace == null ? encodedImage.getColorSpace() : options.colorSpace;
          } else {
            colorSpace = options.colorSpace;
          }
          if (imageFormat == DefaultImageFormats.JPEG) {
            return decodeJpeg(encodedImage, length, qualityInfo, options, colorSpace);
          } else if (imageFormat == DefaultImageFormats.GIF) {
            return decodeGif(encodedImage, length, qualityInfo, options);
          } else if (imageFormat == DefaultImageFormats.WEBP_ANIMATED) {
            return decodeAnimatedWebp(encodedImage, length, qualityInfo, options);
          } else if (imageFormat == DefaultImageFormats.BINARY_XML) {
            return decodeXml(encodedImage, length, qualityInfo, options);
          } else if (imageFormat == ImageFormat.UNKNOWN) {
            throw new DecodeException("unknown image format", encodedImage);
          }
          return decodeStaticImage(encodedImage, options);
        }
      };

  @Nullable private final Map<ImageFormat, ImageDecoder> mCustomDecoders;

  public DefaultImageDecoder(
      @Nullable final ImageDecoder animatedGifDecoder,
      @Nullable final ImageDecoder animatedWebPDecoder,
      @Nullable final ImageDecoder xmlDecoder,
      final PlatformDecoder platformDecoder) {
    this(animatedGifDecoder, animatedWebPDecoder, xmlDecoder, platformDecoder, null);
  }

  public DefaultImageDecoder(
      @Nullable final ImageDecoder animatedGifDecoder,
      @Nullable final ImageDecoder animatedWebPDecoder,
      @Nullable final ImageDecoder xmlDecoder,
      final PlatformDecoder platformDecoder,
      @Nullable Map<ImageFormat, ImageDecoder> customDecoders) {
    this(
        animatedGifDecoder,
        animatedWebPDecoder,
        xmlDecoder,
        platformDecoder,
        customDecoders,
        Suppliers.BOOLEAN_FALSE);
  }

  public DefaultImageDecoder(
      @Nullable final ImageDecoder animatedGifDecoder,
      @Nullable final ImageDecoder animatedWebPDecoder,
      @Nullable final ImageDecoder xmlDecoder,
      final PlatformDecoder platformDecoder,
      @Nullable Map<ImageFormat, ImageDecoder> customDecoders,
      final Supplier<Boolean> enableEncodedImageColorSpaceUsage) {
    mAnimatedGifDecoder = animatedGifDecoder;
    mAnimatedWebPDecoder = animatedWebPDecoder;
    mXmlDecoder = xmlDecoder;
    mPlatformDecoder = platformDecoder;
    mCustomDecoders = customDecoders;
    mEnableEncodedImageColorSpaceUsage = enableEncodedImageColorSpaceUsage;
  }

  /**
   * Decodes image.
   *
   * @param encodedImage input image (encoded bytes plus meta data)
   * @param length if image type supports decoding incomplete image then determines where the image
   *     data should be cut for decoding.
   * @param qualityInfo quality information for the image
   * @param options options that can change decode behavior
   */
  @Override
  public @Nullable CloseableImage decode(
      final EncodedImage encodedImage,
      final int length,
      final QualityInfo qualityInfo,
      final ImageDecodeOptions options) {
    if (options.customImageDecoder != null) {
      return options.customImageDecoder.decode(encodedImage, length, qualityInfo, options);
    }
    ImageFormat imageFormat = encodedImage.getImageFormat();
    if (imageFormat == null || imageFormat == ImageFormat.UNKNOWN) {
      InputStream inputStream = encodedImage.getInputStream();
      if (inputStream != null) {
        imageFormat = ImageFormatChecker.getImageFormat_WrapIOException(inputStream);
        encodedImage.setImageFormat(imageFormat);
      }
    }
    if (mCustomDecoders != null) {
      ImageDecoder decoder = mCustomDecoders.get(imageFormat);
      if (decoder != null) {
        return decoder.decode(encodedImage, length, qualityInfo, options);
      }
    }
    return mDefaultDecoder.decode(encodedImage, length, qualityInfo, options);
  }

  /**
   * Decodes gif into CloseableImage.
   *
   * @param encodedImage input image (encoded bytes plus meta data)
   * @return a CloseableImage
   */
  public @Nullable CloseableImage decodeGif(
      final EncodedImage encodedImage,
      final int length,
      final QualityInfo qualityInfo,
      final ImageDecodeOptions options) {
    if (encodedImage.getWidth() == EncodedImage.UNKNOWN_WIDTH
        || encodedImage.getHeight() == EncodedImage.UNKNOWN_HEIGHT) {
      throw new DecodeException("image width or height is incorrect", encodedImage);
    }
    if (!options.forceStaticImage && mAnimatedGifDecoder != null) {
      return mAnimatedGifDecoder.decode(encodedImage, length, qualityInfo, options);
    }
    return decodeStaticImage(encodedImage, options);
  }

  /**
   * @param encodedImage input image (encoded bytes plus meta data)
   * @return a CloseableStaticBitmap
   */
  public CloseableStaticBitmap decodeStaticImage(
      final EncodedImage encodedImage, ImageDecodeOptions options) {
    CloseableReference<Bitmap> bitmapReference =
        mPlatformDecoder.decodeFromEncodedImageWithColorSpace(
            encodedImage, options.bitmapConfig, null, options.colorSpace);
    try {
      boolean didApplyTransformation =
          TransformationUtils.maybeApplyTransformation(
              options.bitmapTransformation, bitmapReference);

      Preconditions.checkNotNull(bitmapReference);
      CloseableStaticBitmap closeableStaticBitmap =
          CloseableStaticBitmap.of(
              bitmapReference,
              ImmutableQualityInfo.FULL_QUALITY,
              encodedImage.getRotationAngle(),
              encodedImage.getExifOrientation());

      closeableStaticBitmap.putExtra(
          HasExtraData.KEY_IS_ROUNDED,
          didApplyTransformation && options.bitmapTransformation instanceof CircularTransformation);

      return closeableStaticBitmap;
    } finally {
      CloseableReference.closeSafely(bitmapReference);
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
      QualityInfo qualityInfo,
      ImageDecodeOptions options,
      @Nullable ColorSpace colorSpace) {
    CloseableReference<Bitmap> bitmapReference =
        mPlatformDecoder.decodeJPEGFromEncodedImageWithColorSpace(
            encodedImage, options.bitmapConfig, null, length, colorSpace);
    try {
      boolean didApplyTransformation =
          TransformationUtils.maybeApplyTransformation(
              options.bitmapTransformation, bitmapReference);

      Preconditions.checkNotNull(bitmapReference);
      CloseableStaticBitmap closeableStaticBitmap =
          CloseableStaticBitmap.of(
              bitmapReference,
              qualityInfo,
              encodedImage.getRotationAngle(),
              encodedImage.getExifOrientation());

      closeableStaticBitmap.putExtra(
          HasExtraData.KEY_IS_ROUNDED,
          didApplyTransformation && options.bitmapTransformation instanceof CircularTransformation);

      return closeableStaticBitmap;
    } finally {
      CloseableReference.closeSafely(bitmapReference);
    }
  }

  /**
   * Decode a webp animated image into a CloseableImage.
   *
   * <p>The image is decoded into a 'pinned' purgeable bitmap.
   *
   * @param encodedImage input image (encoded bytes plus meta data)
   * @param options
   * @return a {@link CloseableImage}
   */
  public @Nullable CloseableImage decodeAnimatedWebp(
      final EncodedImage encodedImage,
      final int length,
      final QualityInfo qualityInfo,
      final ImageDecodeOptions options) {
    if (!options.forceStaticImage && mAnimatedWebPDecoder != null) {
      return mAnimatedWebPDecoder.decode(encodedImage, length, qualityInfo, options);
    }
    return decodeStaticImage(encodedImage, options);
  }

  /**
   * Decodes a binary xml resource.
   *
   * @param encodedImage input image (encoded bytes plus meta data)
   * @param length amount of currently available data in bytes
   * @param qualityInfo quality info for the image
   * @return a CloseableXml
   */
  private @Nullable CloseableImage decodeXml(
      final EncodedImage encodedImage,
      final int length,
      final QualityInfo qualityInfo,
      final ImageDecodeOptions options) {
    if (mXmlDecoder != null) {
      return mXmlDecoder.decode(encodedImage, length, qualityInfo, options);
    }
    return null;
  }
}

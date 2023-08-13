/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.vito;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import com.facebook.common.references.CloseableReference;
import com.facebook.fresco.middleware.HasExtraData;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.decoder.ImageDecoder;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.CloseableStaticBitmap;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.image.ImmutableQualityInfo;
import com.facebook.imagepipeline.image.QualityInfo;
import com.facebook.imagepipeline.platform.PlatformDecoder;
import com.facebook.imagepipeline.transformation.CircularTransformation;
import com.facebook.imagepipeline.transformation.TransformationUtils;
import javax.annotation.Nullable;

/**
 * Decodes images with an option to decode only a region when {@link
 * com.facebook.drawee.drawable.ScalingUtils.ScaleType} specified.
 */
public class FrescoVitoRegionDecoder implements ImageDecoder {

  private final PlatformDecoder mPlatformDecoder;

  public FrescoVitoRegionDecoder(PlatformDecoder platformDecoder) {
    mPlatformDecoder = platformDecoder;
  }

  /**
   * Decodes a partial jpeg.
   *
   * @param encodedImage input image (encoded bytes plus meta data)
   * @param length if image type supports decoding incomplete image then determines where the image
   *     data should be cut for decoding.
   * @param qualityInfo quality information for the image
   * @param options options that can change decode behavior
   */
  @Override
  public @Nullable CloseableImage decode(
      EncodedImage encodedImage, int length, QualityInfo qualityInfo, ImageDecodeOptions options) {

    Rect regionToDecode = computeRegionToDecode(encodedImage, options);

    CloseableReference<Bitmap> decodedBitmapReference =
        mPlatformDecoder.decodeJPEGFromEncodedImageWithColorSpace(
            encodedImage, options.bitmapConfig, regionToDecode, length, options.colorSpace);
    try {
      boolean didApplyTransformation =
          TransformationUtils.maybeApplyTransformation(
              options.bitmapTransformation, decodedBitmapReference);

      CloseableStaticBitmap closeableStaticBitmap =
          CloseableStaticBitmap.of(
              decodedBitmapReference,
              ImmutableQualityInfo.FULL_QUALITY,
              encodedImage.getRotationAngle(),
              encodedImage.getExifOrientation());

      closeableStaticBitmap.putExtra(
          HasExtraData.KEY_IS_ROUNDED,
          didApplyTransformation && options.bitmapTransformation instanceof CircularTransformation);

      return closeableStaticBitmap;
    } finally {
      CloseableReference.closeSafely(decodedBitmapReference);
    }
  }

  private @Nullable Rect computeRegionToDecode(
      EncodedImage encodedImage, ImageDecodeOptions options) {
    if (!(options instanceof FrescoVitoImageDecodeOptions)) {
      return null;
    }
    FrescoVitoImageDecodeOptions frescoVitoOptions = (FrescoVitoImageDecodeOptions) options;

    Rect regionToDecode = new Rect();
    Matrix matrix = new Matrix();
    frescoVitoOptions.scaleType.getTransform(
        matrix,
        frescoVitoOptions.parentBounds,
        encodedImage.getWidth(),
        encodedImage.getHeight(),
        frescoVitoOptions.focusPoint.x,
        frescoVitoOptions.focusPoint.y);
    matrix.invert(matrix);

    RectF tempRectangle = new RectF(frescoVitoOptions.parentBounds);
    matrix.mapRect(tempRectangle);

    tempRectangle.round(regionToDecode);
    return regionToDecode;
  }
}

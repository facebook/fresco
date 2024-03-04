/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.webpsupport;

import android.graphics.Bitmap;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.bitmaps.SimpleBitmapReleaser;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.decoder.ImageDecoder;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.CloseableStaticBitmap;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.image.QualityInfo;
import com.facebook.infer.annotation.Nullsafe;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class WebPImageDecoder implements ImageDecoder {

  @Override
  public @Nullable CloseableImage decode(
      EncodedImage encodedImage, int length, QualityInfo qualityInfo, ImageDecodeOptions options) {

    final CloseableReference<PooledByteBuffer> bytesRef = encodedImage.getByteBufferRef();
    Preconditions.checkNotNull(bytesRef);
    try {
      final PooledByteBuffer input = bytesRef.get();
      final ByteBuffer buffer = input.getByteBuffer();
      if (buffer != null) {
        return decodeByteBuffer(buffer, encodedImage, length, qualityInfo, options);
      }
    } finally {
      CloseableReference.closeSafely(bytesRef);
    }

    return decodeInputStream(encodedImage, length, qualityInfo, options);
  }

  @Nullable
  private static CloseableImage decodeByteBuffer(
      ByteBuffer byteBuffer,
      EncodedImage encodedImage,
      int length,
      QualityInfo qualityInfo,
      ImageDecodeOptions options) {
    Bitmap bitmap = WebpBitmapFactoryImpl.hookDecodeByteArray(byteBuffer.array(), 0, length);
    return bitmapToCloseableImage(bitmap, encodedImage, qualityInfo);
  }

  @Nullable
  private static CloseableImage decodeInputStream(
      EncodedImage encodedImage, int length, QualityInfo qualityInfo, ImageDecodeOptions options) {
    try (InputStream is = encodedImage.getInputStreamOrThrow()) {
      Bitmap bitmap = WebpBitmapFactoryImpl.hookDecodeStream(is, null, null);
      return bitmapToCloseableImage(bitmap, encodedImage, qualityInfo);
    } catch (IOException e) {
      throw new RuntimeException("Error while decoding WebP", e);
    }
  }

  @Nullable
  private static CloseableImage bitmapToCloseableImage(
      @Nullable Bitmap bitmap, EncodedImage encodedImage, QualityInfo qualityInfo) {
    if (bitmap == null) {
      return null;
    }
    final CloseableReference<Bitmap> bitmapRef =
        CloseableReference.of(bitmap, SimpleBitmapReleaser.getInstance());
    return CloseableStaticBitmap.of(bitmapRef, qualityInfo, encodedImage.getRotationAngle());
  }
}

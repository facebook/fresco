/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import java.io.InputStream;
import java.util.concurrent.Executor;

import com.facebook.common.references.CloseableReference;
import com.facebook.common.util.TriState;
import com.facebook.imageformat.ImageFormat;
import com.facebook.imageformat.ImageFormatChecker;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.memory.PooledByteBufferInputStream;
import com.facebook.imagepipeline.memory.PooledByteBufferFactory;
import com.facebook.imagepipeline.memory.PooledByteBufferOutputStream;
import com.facebook.imagepipeline.nativecode.WebpTranscoder;
import com.facebook.imagepipeline.request.ImageRequest;

/**
 * Transcodes webps to jpegs.
 *
 * <p> If processed image is one of VP8, VP8X or VP8L non-animated webps then
 * it is transcoded to jpeg if Android's decoder does not support this format.
 * If image is of any other type, no transformation is applied.
 */
public class WebpTranscodeProducer
    extends ImageTransformProducer<CloseableReference<PooledByteBuffer>, Void> {
  private static final String PRODUCER_NAME = "WebpTranscodeProducer";
  private static final int DEFAULT_JPEG_QUALITY = 80;

  public WebpTranscodeProducer(
      Executor executor,
      PooledByteBufferFactory pooledByteBufferFactory,
      Producer<CloseableReference<PooledByteBuffer>> nextProducer) {
    super(executor, pooledByteBufferFactory, nextProducer);
  }

  @Override
  protected TriState shouldTransform(
      final CloseableReference<PooledByteBuffer> imageRef,
      final ImageRequest imageRequest,
      boolean isLast) {
    InputStream imageInputStream = new PooledByteBufferInputStream(imageRef.get());
    ImageFormat imageFormat = ImageFormatChecker.getImageFormat_WrapIOException(imageInputStream);

    switch (imageFormat) {
      case WEBP_SIMPLE:
      case WEBP_LOSSLESS:
      case WEBP_EXTENDED:
      case WEBP_EXTENDED_WITH_ALPHA:
        return TriState.valueOf(!WebpTranscoder.isWebpNativelySupported(imageFormat));
      case UNKNOWN:
        return isLast ? TriState.NO : TriState.UNSET;
      default:
        return TriState.NO;
    }
  }

  @Override
  protected void transform(
      final CloseableReference<PooledByteBuffer> imageRef,
      final PooledByteBufferOutputStream outputStream,
      ImageRequest imageRequest,
      Void unused) throws Exception {
    InputStream imageInputStream = new PooledByteBufferInputStream(imageRef.get());
    ImageFormat imageFormat = ImageFormatChecker.getImageFormat_WrapIOException(imageInputStream);
    switch (imageFormat) {
      case WEBP_SIMPLE:
      case WEBP_EXTENDED:
        WebpTranscoder.transcodeWebpToJpeg(imageInputStream, outputStream, DEFAULT_JPEG_QUALITY);
        break;

      case WEBP_LOSSLESS:
      case WEBP_EXTENDED_WITH_ALPHA:
        WebpTranscoder.transcodeWebpToPng(imageInputStream, outputStream);
        break;

      default:
        throw new IllegalArgumentException("Wrong image format");
    }
  }

  @Override
  protected CloseableReference<PooledByteBuffer> getImageCopy(
      CloseableReference<PooledByteBuffer> originalResult) {
    return originalResult.clone();
  }

  @Override
  protected Void getExtraInformation(
      CloseableReference<PooledByteBuffer> originalResult) {
    return null;
  }

  @Override
  protected CloseableReference<PooledByteBuffer> createReturnValue(
      PooledByteBuffer transformedBytes,
      Void unused) {
    return CloseableReference.of(transformedBytes);
  }

  @Override
  protected void closeReturnValue(CloseableReference<PooledByteBuffer> returnValue) {
    CloseableReference.closeSafely(returnValue);
  }

  @Override
  protected String getProducerName() {
    return PRODUCER_NAME;
  }

  @Override
  protected boolean shouldAllowCancellation() {
    return false;
  }
}

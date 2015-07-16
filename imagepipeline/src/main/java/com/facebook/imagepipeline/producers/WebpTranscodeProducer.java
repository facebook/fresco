/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import javax.annotation.Nullable;

import java.io.InputStream;
import java.util.concurrent.Executor;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.util.TriState;
import com.facebook.imageformat.ImageFormat;
import com.facebook.imageformat.ImageFormatChecker;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.memory.PooledByteBufferFactory;
import com.facebook.imagepipeline.memory.PooledByteBufferOutputStream;
import com.facebook.imagepipeline.nativecode.WebpTranscoder;

/**
 * Transcodes WebP to JPEG / PNG.
 *
 * <p> If processed image is one of VP8, VP8X or VP8L non-animated WebPs then it is transcoded to
 * jpeg if the decoder on the running version of Android does not support this format. This was the
 * case prior to version 4.2.1.
 * <p> If the image is not WebP, no transformation is applied.
 */
public class WebpTranscodeProducer implements Producer<EncodedImage> {
  private static final String PRODUCER_NAME = "WebpTranscodeProducer";
  private static final int DEFAULT_JPEG_QUALITY = 80;

  private final Executor mExecutor;
  private final PooledByteBufferFactory mPooledByteBufferFactory;
  private final Producer<EncodedImage> mNextProducer;

  public WebpTranscodeProducer(
      Executor executor,
      PooledByteBufferFactory pooledByteBufferFactory,
      Producer<EncodedImage> nextProducer) {
    mExecutor = Preconditions.checkNotNull(executor);
    mPooledByteBufferFactory = Preconditions.checkNotNull(pooledByteBufferFactory);
    mNextProducer = Preconditions.checkNotNull(nextProducer);
  }

  @Override
  public void produceResults(final Consumer<EncodedImage> consumer, final ProducerContext context) {
    mNextProducer.produceResults(new WebpTranscodeConsumer(consumer, context), context);
  }

  private class WebpTranscodeConsumer extends DelegatingConsumer<EncodedImage, EncodedImage> {
    private final ProducerContext mContext;
    private TriState mShouldTranscodeWhenFinished;

    public WebpTranscodeConsumer(
        final Consumer<EncodedImage> consumer,
        final ProducerContext context) {
      super(consumer);
      mContext = context;
      mShouldTranscodeWhenFinished = TriState.UNSET;
    }

    @Override
    protected void onNewResultImpl(@Nullable EncodedImage newResult, boolean isLast) {
      // try to determine if the last result should be transformed
      if (mShouldTranscodeWhenFinished == TriState.UNSET && newResult != null) {
        mShouldTranscodeWhenFinished = shouldTranscode(newResult);
      }

      // just propagate result if it shouldn't be transformed
      if (mShouldTranscodeWhenFinished == TriState.NO) {
        getConsumer().onNewResult(newResult, isLast);
        return;
      }

      if (isLast) {
        if (mShouldTranscodeWhenFinished == TriState.YES && newResult != null) {
          transcodeLastResult(newResult, getConsumer(), mContext);
        } else {
          getConsumer().onNewResult(newResult, isLast);
        }
      }
    }
  }

  private void transcodeLastResult(
      final EncodedImage originalResult,
      final Consumer<EncodedImage> consumer,
      final ProducerContext producerContext) {
    Preconditions.checkNotNull(originalResult);
    final EncodedImage encodedImageCopy = EncodedImage.cloneOrNull(originalResult);
    final StatefulProducerRunnable<EncodedImage> runnable =
        new StatefulProducerRunnable<EncodedImage>(
            consumer,
            producerContext.getListener(),
            PRODUCER_NAME,
            producerContext.getId()) {
          @Override
          protected EncodedImage getResult() throws Exception {
            PooledByteBufferOutputStream outputStream = mPooledByteBufferFactory.newOutputStream();
            try {
              doTranscode(encodedImageCopy, outputStream);
              CloseableReference<PooledByteBuffer> ref =
                  CloseableReference.of(outputStream.toByteBuffer());
              try {
                EncodedImage encodedImage = new EncodedImage(ref);
                encodedImage.copyMetaDataFrom(encodedImageCopy);
                return encodedImage;
              } finally {
                CloseableReference.closeSafely(ref);
              }
            } finally {
              outputStream.close();
            }
          }

          @Override
          protected void disposeResult(EncodedImage result) {
            EncodedImage.closeSafely(result);
          }

          @Override
          protected void onSuccess(EncodedImage result) {
            EncodedImage.closeSafely(encodedImageCopy);
            super.onSuccess(result);
          }

          @Override
          protected void onFailure(Exception e) {
            EncodedImage.closeSafely(encodedImageCopy);
            super.onFailure(e);
          }

          @Override
          protected void onCancellation() {
            EncodedImage.closeSafely(encodedImageCopy);
            super.onCancellation();
          }
        };
    mExecutor.execute(runnable);
  }

  private static TriState shouldTranscode(final EncodedImage encodedImage) {
    Preconditions.checkNotNull(encodedImage);
    ImageFormat imageFormat = ImageFormatChecker.getImageFormat_WrapIOException(
        encodedImage.getInputStream());
    switch (imageFormat) {
      case WEBP_SIMPLE:
      case WEBP_LOSSLESS:
      case WEBP_EXTENDED:
      case WEBP_EXTENDED_WITH_ALPHA:
        return TriState.valueOf(!WebpTranscoder.isWebpNativelySupported(imageFormat));
      case UNKNOWN:
        // the image format might be unknown because we haven't fetched the whole header yet,
        // in which case the decision whether to transcode or not cannot be made yet
        return TriState.UNSET;
      default:
        // if the image format is known, but it is not WebP, then the image shouldn't be transcoded
        return TriState.NO;
    }
  }

  private static void doTranscode(
      final EncodedImage encodedImage,
      final PooledByteBufferOutputStream outputStream) throws Exception {
    InputStream imageInputStream = encodedImage.getInputStream();
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
}

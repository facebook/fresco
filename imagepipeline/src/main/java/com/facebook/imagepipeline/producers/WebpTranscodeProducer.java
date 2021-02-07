/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.memory.PooledByteBufferFactory;
import com.facebook.common.memory.PooledByteBufferOutputStream;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.util.TriState;
import com.facebook.imageformat.DefaultImageFormats;
import com.facebook.imageformat.ImageFormat;
import com.facebook.imageformat.ImageFormatChecker;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.nativecode.WebpTranscoder;
import com.facebook.imagepipeline.nativecode.WebpTranscoderFactory;
import java.io.InputStream;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;

/**
 * Transcodes WebP to JPEG / PNG.
 *
 * <p>If processed image is one of VP8, VP8X or VP8L non-animated WebPs then it is transcoded to
 * jpeg if the decoder on the running version of Android does not support this format. This was the
 * case prior to version 4.2.1.
 *
 * <p>If the image is not WebP, no transformation is applied.
 */
public class WebpTranscodeProducer implements Producer<EncodedImage> {
  public static final String PRODUCER_NAME = "WebpTranscodeProducer";

  private static final int DEFAULT_JPEG_QUALITY = 80;

  private final Executor mExecutor;
  private final PooledByteBufferFactory mPooledByteBufferFactory;
  private final Producer<EncodedImage> mInputProducer;

  public WebpTranscodeProducer(
      Executor executor,
      PooledByteBufferFactory pooledByteBufferFactory,
      Producer<EncodedImage> inputProducer) {
    mExecutor = Preconditions.checkNotNull(executor);
    mPooledByteBufferFactory = Preconditions.checkNotNull(pooledByteBufferFactory);
    mInputProducer = Preconditions.checkNotNull(inputProducer);
  }

  @Override
  public void produceResults(final Consumer<EncodedImage> consumer, final ProducerContext context) {
    mInputProducer.produceResults(new WebpTranscodeConsumer(consumer, context), context);
  }

  private class WebpTranscodeConsumer extends DelegatingConsumer<EncodedImage, EncodedImage> {
    private final ProducerContext mContext;
    private TriState mShouldTranscodeWhenFinished;

    public WebpTranscodeConsumer(
        final Consumer<EncodedImage> consumer, final ProducerContext context) {
      super(consumer);
      mContext = context;
      mShouldTranscodeWhenFinished = TriState.UNSET;
    }

    @Override
    protected void onNewResultImpl(@Nullable EncodedImage newResult, @Status int status) {
      // try to determine if the last result should be transformed
      if (mShouldTranscodeWhenFinished == TriState.UNSET && newResult != null) {
        mShouldTranscodeWhenFinished = shouldTranscode(newResult);
      }

      // just propagate result if it shouldn't be transformed
      if (mShouldTranscodeWhenFinished == TriState.NO) {
        getConsumer().onNewResult(newResult, status);
        return;
      }

      if (isLast(status)) {
        if (mShouldTranscodeWhenFinished == TriState.YES && newResult != null) {
          transcodeLastResult(newResult, getConsumer(), mContext);
        } else {
          getConsumer().onNewResult(newResult, status);
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
            consumer, producerContext.getProducerListener(), producerContext, PRODUCER_NAME) {
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
    ImageFormat imageFormat =
        ImageFormatChecker.getImageFormat_WrapIOException(encodedImage.getInputStream());
    if (DefaultImageFormats.isStaticWebpFormat(imageFormat)) {
      final WebpTranscoder webpTranscoder = WebpTranscoderFactory.getWebpTranscoder();
      if (webpTranscoder == null) {
        return TriState.NO;
      }
      return TriState.valueOf(!webpTranscoder.isWebpNativelySupported(imageFormat));
    } else if (imageFormat == ImageFormat.UNKNOWN) {
      // the image format might be unknown because we haven't fetched the whole header yet,
      // in which case the decision whether to transcode or not cannot be made yet
      return TriState.UNSET;
    }
    // if the image format is known, but it is not WebP, then the image shouldn't be transcoded
    return TriState.NO;
  }

  private static void doTranscode(
      final EncodedImage encodedImage, final PooledByteBufferOutputStream outputStream)
      throws Exception {
    InputStream imageInputStream = encodedImage.getInputStream();
    ImageFormat imageFormat = ImageFormatChecker.getImageFormat_WrapIOException(imageInputStream);
    if (imageFormat == DefaultImageFormats.WEBP_SIMPLE
        || imageFormat == DefaultImageFormats.WEBP_EXTENDED) {
      WebpTranscoderFactory.getWebpTranscoder()
          .transcodeWebpToJpeg(imageInputStream, outputStream, DEFAULT_JPEG_QUALITY);
      encodedImage.setImageFormat(DefaultImageFormats.JPEG);
    } else if (imageFormat == DefaultImageFormats.WEBP_LOSSLESS
        || imageFormat == DefaultImageFormats.WEBP_EXTENDED_WITH_ALPHA) {
      // In this case we always transcode to PNG
      WebpTranscoderFactory.getWebpTranscoder().transcodeWebpToPng(imageInputStream, outputStream);
      encodedImage.setImageFormat(DefaultImageFormats.PNG);
    } else {
      throw new IllegalArgumentException("Wrong image format");
    }
  }
}

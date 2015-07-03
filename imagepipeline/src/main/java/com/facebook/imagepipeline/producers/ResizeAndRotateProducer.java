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

import java.util.Map;
import java.util.concurrent.Executor;

import com.facebook.common.internal.ImmutableMap;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.util.TriState;
import com.facebook.imageformat.ImageFormat;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.memory.PooledByteBufferFactory;
import com.facebook.imagepipeline.memory.PooledByteBufferOutputStream;
import com.facebook.imagepipeline.nativecode.JpegTranscoder;
import com.facebook.imagepipeline.request.ImageRequest;

/**
 * Resizes and rotates JPEG image according to the EXIF orientation data.
 *
 * <p> If the image is not JPEG, no transformation is applied.
 */
public class ResizeAndRotateProducer implements Producer<EncodedImage> {
  private static final String PRODUCER_NAME = "ResizeAndRotateProducer";
  private static final String ORIGINAL_SIZE_KEY = "Original size";
  private static final String REQUESTED_SIZE_KEY = "Requested size";
  private static final String FRACTION_KEY = "Fraction";

  @VisibleForTesting static final int DEFAULT_JPEG_QUALITY = 85;
  @VisibleForTesting static final int MAX_JPEG_SCALE_NUMERATOR = JpegTranscoder.SCALE_DENOMINATOR;
  @VisibleForTesting static final int MIN_TRANSFORM_INTERVAL_MS = 100;

  private static final float MAX_BITMAP_SIZE = 2048f;
  private static final float ROUNDUP_FRACTION = 2.0f/3;

  private final Executor mExecutor;
  private final PooledByteBufferFactory mPooledByteBufferFactory;
  private final boolean mDownsampleEnabled;
  private final Producer<EncodedImage> mNextProducer;

  public ResizeAndRotateProducer(
      Executor executor,
      PooledByteBufferFactory pooledByteBufferFactory,
      boolean downsampleEnabled,
      Producer<EncodedImage> nextProducer) {
    mExecutor = Preconditions.checkNotNull(executor);
    mPooledByteBufferFactory = Preconditions.checkNotNull(pooledByteBufferFactory);
    mDownsampleEnabled = downsampleEnabled;
    mNextProducer = Preconditions.checkNotNull(nextProducer);
  }

  @Override
  public void produceResults(
      final Consumer<EncodedImage> consumer,
      final ProducerContext context) {
    mNextProducer.produceResults(new TransformingConsumer(consumer, context), context);
  }

  private class TransformingConsumer extends DelegatingConsumer<EncodedImage, EncodedImage> {

    private final ProducerContext mProducerContext;
    private boolean mIsCancelled;

    private final JobScheduler mJobScheduler;

    public TransformingConsumer(
        final Consumer<EncodedImage> consumer,
        final ProducerContext producerContext) {
      super(consumer);
      mIsCancelled = false;
      mProducerContext = producerContext;

      JobScheduler.JobRunnable job = new JobScheduler.JobRunnable() {
        @Override
        public void run(EncodedImage encodedImage, boolean isLast) {
          doTransform(encodedImage, isLast);
        }
      };
      mJobScheduler = new JobScheduler(mExecutor, job, MIN_TRANSFORM_INTERVAL_MS);

      mProducerContext.addCallbacks(
          new BaseProducerContextCallbacks() {
            @Override
            public void onIsIntermediateResultExpectedChanged() {
              if (mProducerContext.isIntermediateResultExpected()) {
                mJobScheduler.scheduleJob();
              }
            }
            @Override
            public void onCancellationRequested() {
              mJobScheduler.clearJob();
              mIsCancelled = true;
              // this only works if it is safe to discard the output of previous producer
              consumer.onCancellation();
            }
          });
    }

    @Override
    protected void onNewResultImpl(@Nullable EncodedImage newResult, boolean isLast) {
      if (mIsCancelled) {
        return;
      }
      if (newResult == null) {
        if (isLast) {
          getConsumer().onNewResult(null, true);
        }
        return;
      }
      TriState shouldTransform =
          shouldTransform(mProducerContext.getImageRequest(), newResult, mDownsampleEnabled);
      // ignore the intermediate result if we don't know what to do with it
      if (!isLast && shouldTransform == TriState.UNSET) {
        return;
      }
      // just forward the result if we know that it shouldn't be transformed
      if (shouldTransform != TriState.YES) {
        getConsumer().onNewResult(newResult, isLast);
        return;
      }
      // we know that the result should be transformed, hence schedule it
      if (!mJobScheduler.updateJob(newResult, isLast)) {
        return;
      }
      if (isLast || mProducerContext.isIntermediateResultExpected()) {
        mJobScheduler.scheduleJob();
      }
    }

    private void doTransform(EncodedImage encodedImage, boolean isLast) {
      mProducerContext.getListener().onProducerStart(mProducerContext.getId(), PRODUCER_NAME);
      ImageRequest imageRequest = mProducerContext.getImageRequest();
      PooledByteBufferOutputStream outputStream = mPooledByteBufferFactory.newOutputStream();
      Map<String, String> extraMap = null;
      EncodedImage ret = null;
      try {
        int numerator = getScaleNumerator(imageRequest, encodedImage);
        extraMap = getExtraMap(encodedImage, imageRequest, numerator);
        JpegTranscoder.transcodeJpeg(
            encodedImage.getInputStream(),
            outputStream,
            getRotationAngle(imageRequest, encodedImage),
            numerator,
            DEFAULT_JPEG_QUALITY);
        CloseableReference<PooledByteBuffer> ref =
            CloseableReference.of(outputStream.toByteBuffer());
        try {
          ret = new EncodedImage(ref);
          ret.setImageFormat(ImageFormat.JPEG);
          try {
            ret.parseMetaData();
            mProducerContext.getListener().
                onProducerFinishWithSuccess(mProducerContext.getId(), PRODUCER_NAME, extraMap);
            getConsumer().onNewResult(ret, isLast);
          } finally {
            EncodedImage.closeSafely(ret);
          }
        } finally {
          CloseableReference.closeSafely(ref);
        }
      } catch (Exception e) {
        mProducerContext.getListener().
            onProducerFinishWithFailure(mProducerContext.getId(), PRODUCER_NAME, e, extraMap);
        getConsumer().onFailure(e);
        return;
      } finally {
        outputStream.close();
      }
    }

    private Map<String, String> getExtraMap(
        EncodedImage encodedImage,
        ImageRequest imageRequest,
        int numerator) {
      if (!mProducerContext.getListener().requiresExtraMap(mProducerContext.getId())) {
        return null;
      }
      String originalSize = encodedImage.getWidth() + "x" + encodedImage.getHeight();
      String requestedSize =
          imageRequest.getResizeOptions().width + "x" + imageRequest.getResizeOptions().height;
      String fraction = numerator > 0 ? numerator + "/8" : "";
      return ImmutableMap.of(
          ORIGINAL_SIZE_KEY, originalSize,
          REQUESTED_SIZE_KEY, requestedSize,
          FRACTION_KEY, fraction,
          JobScheduler.QUEUE_TIME_KEY, String.valueOf(mJobScheduler.getQueuedTime()));
    }
  }

  private static TriState shouldTransform(
      ImageRequest request,
      EncodedImage encodedImage,
      boolean downsampleEnabled) {
    if (encodedImage == null || encodedImage.getImageFormat() == ImageFormat.UNKNOWN) {
      return TriState.UNSET;
    }
    if (encodedImage.getImageFormat() != ImageFormat.JPEG) {
      return TriState.NO;
    }
    return TriState.valueOf(
        getRotationAngle(request, encodedImage) != 0 ||
            shouldResize(getScaleNumerator(request, encodedImage), downsampleEnabled));
  }

  @VisibleForTesting static float determineResizeRatio(
      ResizeOptions resizeOptions,
      int width,
      int height) {
    final float widthRatio = ((float) resizeOptions.width) / width;
    final float heightRatio = ((float) resizeOptions.height) / height;
    float ratio = Math.max(widthRatio, heightRatio);

    // TODO: The limit is larger than this on newer devices. The problem is to get the real limit,
    // you have to call Canvas.getMaximumBitmapWidth/Height on a real HW-accelerated Canvas.
    if (width * ratio > MAX_BITMAP_SIZE) {
      ratio = MAX_BITMAP_SIZE / width;
    }
    if (height * ratio > MAX_BITMAP_SIZE) {
      ratio = MAX_BITMAP_SIZE / height;
    }
    return ratio;
  }

  @VisibleForTesting static int roundNumerator(float maxRatio) {
    return (int) (ROUNDUP_FRACTION + maxRatio * JpegTranscoder.SCALE_DENOMINATOR);
  }

  private static int getScaleNumerator(
      ImageRequest imageRequest,
      EncodedImage encodedImage) {
    final ResizeOptions resizeOptions = imageRequest.getResizeOptions();
    if (resizeOptions == null) {
      return JpegTranscoder.SCALE_DENOMINATOR;
    }

    final int rotationAngle = getRotationAngle(imageRequest, encodedImage);
    final boolean swapDimensions = rotationAngle == 90 || rotationAngle == 270;
    final int widthAfterRotation = swapDimensions ? encodedImage.getHeight() :
            encodedImage.getWidth();
    final int heightAfterRotation = swapDimensions ? encodedImage.getWidth() :
            encodedImage.getHeight();

    float ratio = determineResizeRatio(resizeOptions, widthAfterRotation, heightAfterRotation);
    int numerator = roundNumerator(ratio);
    if (numerator > MAX_JPEG_SCALE_NUMERATOR) {
      return MAX_JPEG_SCALE_NUMERATOR;
    }
    return (numerator < 1) ? 1 : numerator;
  }

  private static int getRotationAngle(ImageRequest imageRequest, EncodedImage encodedImage) {
    if (!imageRequest.getAutoRotateEnabled()) {
      return 0;
    }
    int rotationAngle = encodedImage.getRotationAngle();
    Preconditions.checkArgument(
        rotationAngle == 0 || rotationAngle == 90 || rotationAngle == 180 || rotationAngle == 270);
    return rotationAngle;
  }

  private static boolean shouldResize(int numerator, boolean downsampleEnabled) {
    return !(downsampleEnabled && numerator <= (MAX_JPEG_SCALE_NUMERATOR / 2))
        && numerator < MAX_JPEG_SCALE_NUMERATOR;
  }
}

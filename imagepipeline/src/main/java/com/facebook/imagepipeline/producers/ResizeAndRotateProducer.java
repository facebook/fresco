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

import java.util.concurrent.Executor;

import android.util.Pair;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.util.TriState;
import com.facebook.imageformat.ImageFormat;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.memory.PooledByteBufferFactory;
import com.facebook.imagepipeline.memory.PooledByteBufferInputStream;
import com.facebook.imagepipeline.memory.PooledByteBufferOutputStream;
import com.facebook.imagepipeline.nativecode.JpegTranscoder;
import com.facebook.imagepipeline.request.ImageRequest;

/**
 * Resizes and rotates JPEG image according to the EXIF orientation data.
 *
 * <p> If the image is not JPEG, no transformation is applied.
 */
public class ResizeAndRotateProducer
    implements Producer<Pair<CloseableReference<PooledByteBuffer>, ImageTransformMetaData>> {
  private static final String PRODUCER_NAME = "ResizeAndRotateProducer";
  @VisibleForTesting static final int DEFAULT_JPEG_QUALITY = 85;
  @VisibleForTesting static final int MAX_JPEG_SCALE_NUMERATOR = JpegTranscoder.SCALE_DENOMINATOR;
  @VisibleForTesting static final int MIN_TRANSFORM_INTERVAL_MS = 100;

  private static final float MAX_BITMAP_SIZE = 2048f;

  private final Executor mExecutor;
  private final PooledByteBufferFactory mPooledByteBufferFactory;
  private final Producer<Pair<CloseableReference<PooledByteBuffer>, ImageTransformMetaData>>
      mNextProducer;

  public ResizeAndRotateProducer(
      Executor executor,
      PooledByteBufferFactory pooledByteBufferFactory,
      Producer<Pair<CloseableReference<PooledByteBuffer>, ImageTransformMetaData>> nextProducer) {
    mExecutor = Preconditions.checkNotNull(executor);
    mPooledByteBufferFactory = Preconditions.checkNotNull(pooledByteBufferFactory);
    mNextProducer = Preconditions.checkNotNull(nextProducer);
  }

  @Override
  public void produceResults(
      final Consumer<Pair<CloseableReference<PooledByteBuffer>, ImageTransformMetaData>> consumer,
      final ProducerContext context) {
    mNextProducer.produceResults(new TransformingConsumer(consumer, context), context);
  }

  private class TransformingConsumer extends DelegatingConsumer<
      Pair<CloseableReference<PooledByteBuffer>, ImageTransformMetaData>,
      Pair<CloseableReference<PooledByteBuffer>, ImageTransformMetaData>> {

    private final ProducerContext mProducerContext;
    private final JobScheduler<PooledByteBuffer, ImageTransformMetaData> mJobScheduler;
    private boolean mIsCancelled;

    public TransformingConsumer(
        final Consumer<Pair<CloseableReference<PooledByteBuffer>, ImageTransformMetaData>> consumer,
        final ProducerContext producerContext) {
      super(consumer);
      mIsCancelled = false;
      mProducerContext = producerContext;
      JobScheduler.JobRunnable<PooledByteBuffer, ImageTransformMetaData>
          job = new JobScheduler.JobRunnable<PooledByteBuffer, ImageTransformMetaData>() {
        @Override
        public void run(
            CloseableReference<PooledByteBuffer> inputRef,
            ImageTransformMetaData metaData,
            boolean isLast) {
          doTransform(inputRef, metaData, isLast);
        }
      };
      mJobScheduler = new JobScheduler<>(mExecutor, job, MIN_TRANSFORM_INTERVAL_MS);
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
    protected void onNewResultImpl(
        @Nullable Pair<CloseableReference<PooledByteBuffer>, ImageTransformMetaData> newResult,
        boolean isLast) {
      if (mIsCancelled) {
        return;
      }
      if (newResult == null) {
        if (isLast) {
          getConsumer().onNewResult(null, true);
        }
        return;
      }
      CloseableReference<PooledByteBuffer> inputRef = newResult.first;
      ImageTransformMetaData metaData = newResult.second;
      TriState shouldTransform = shouldTransform(mProducerContext.getImageRequest(), metaData);
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
      if (!mJobScheduler.updateJob(inputRef, metaData, isLast)) {
        return;
      }
      if (isLast || mProducerContext.isIntermediateResultExpected()) {
        mJobScheduler.scheduleJob();
      }
    }

    private void doTransform(
        CloseableReference<PooledByteBuffer> inputRef,
        ImageTransformMetaData metaData,
        boolean isLast) {
      mProducerContext.getListener().onProducerStart(mProducerContext.getId(), PRODUCER_NAME);
      ImageRequest imageRequest = mProducerContext.getImageRequest();
      Pair<CloseableReference<PooledByteBuffer>, ImageTransformMetaData> ret;
      PooledByteBufferOutputStream outputStream = mPooledByteBufferFactory.newOutputStream();
      try {
        JpegTranscoder.transcodeJpeg(
            new PooledByteBufferInputStream(inputRef.get()),
            outputStream,
            getRotationAngle(imageRequest, metaData),
            getScaleNumerator(imageRequest, metaData),
            DEFAULT_JPEG_QUALITY);
        // TODO t7065568: metaData is no longer up to date!
        ret = Pair.create(CloseableReference.of(outputStream.toByteBuffer()), metaData);

        try {
          mProducerContext.getListener().
              onProducerFinishWithSuccess(mProducerContext.getId(), PRODUCER_NAME, null);
          getConsumer().onNewResult(ret, isLast);
        } finally {
          CloseableReference.closeSafely(ret.first);
        }
      } catch (Exception e) {
        mProducerContext.getListener().
            onProducerFinishWithFailure(mProducerContext.getId(), PRODUCER_NAME, e, null);
        getConsumer().onFailure(e);
        return;
      } finally {
        outputStream.close();
      }
    }
  }

  private static TriState shouldTransform(ImageRequest request, ImageTransformMetaData metaData) {
    if (metaData == null || metaData.getImageFormat() == ImageFormat.UNKNOWN) {
      return TriState.UNSET;
    }
    if (metaData.getImageFormat() != ImageFormat.JPEG) {
      return TriState.NO;
    }
    return TriState.valueOf(
        getRotationAngle(request, metaData) != 0 ||
        getScaleNumerator(request, metaData) != JpegTranscoder.SCALE_DENOMINATOR);
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
    return (int) (0.75f + maxRatio * JpegTranscoder.SCALE_DENOMINATOR);
  }

  private static int getScaleNumerator(ImageRequest imageRequest, ImageTransformMetaData metaData) {
    final ResizeOptions resizeOptions = imageRequest.getResizeOptions();
    if (resizeOptions == null) {
      return JpegTranscoder.SCALE_DENOMINATOR;
    }

    final int rotationAngle = getRotationAngle(imageRequest, metaData);
    final boolean swapDimensions = rotationAngle == 90 || rotationAngle == 270;
    final int widthAfterRotation = swapDimensions ? metaData.getHeight() : metaData.getWidth();
    final int heightAfterRotation = swapDimensions ? metaData.getWidth() : metaData.getHeight();

    float ratio = determineResizeRatio(resizeOptions, widthAfterRotation, heightAfterRotation);

    int numerator = roundNumerator(ratio);

    if (numerator > MAX_JPEG_SCALE_NUMERATOR) {
      return MAX_JPEG_SCALE_NUMERATOR;
    }
    if (numerator < 1) {
      return 1;
    }
    return numerator;
  }

  private static int getRotationAngle(ImageRequest imageRequest, ImageTransformMetaData metaData) {
    if (!imageRequest.getAutoRotateEnabled()) {
      return 0;
    }
    int rotationAngle = metaData.getRotationAngle();
    Preconditions.checkArgument(
        rotationAngle == 0 || rotationAngle == 90 || rotationAngle == 180 || rotationAngle == 270);
    return rotationAngle;
  }
}

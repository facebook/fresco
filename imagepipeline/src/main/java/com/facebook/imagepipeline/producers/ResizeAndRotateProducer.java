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
import java.util.Map;
import java.util.concurrent.Executor;

import com.facebook.common.internal.Closeables;
import com.facebook.common.internal.ImmutableMap;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.util.TriState;
import com.facebook.imageformat.ImageFormat;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.common.RotationOptions;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.memory.PooledByteBufferFactory;
import com.facebook.imagepipeline.memory.PooledByteBufferOutputStream;
import com.facebook.imagepipeline.nativecode.JpegTranscoder;
import com.facebook.imagepipeline.request.ImageRequest;

/**
 * Resizes and rotates JPEG image according to the EXIF orientation data or a specified rotation
 * angle.
 *
 * <p> If the image is not JPEG, no transformation is applied.
 *
 * <p> This can be used even if downsampling is enabled as long as resizing is disabled in the
 * constructor.
 */
public class ResizeAndRotateProducer implements Producer<EncodedImage> {
  public static final String PRODUCER_NAME = "ResizeAndRotateProducer";
  private static final String ORIGINAL_SIZE_KEY = "Original size";
  private static final String REQUESTED_SIZE_KEY = "Requested size";
  private static final String FRACTION_KEY = "Fraction";

  @VisibleForTesting static final int DEFAULT_JPEG_QUALITY = 85;
  @VisibleForTesting static final int MAX_JPEG_SCALE_NUMERATOR = JpegTranscoder.SCALE_DENOMINATOR;
  @VisibleForTesting static final int MIN_TRANSFORM_INTERVAL_MS = 100;

  private final Executor mExecutor;
  private final PooledByteBufferFactory mPooledByteBufferFactory;
  private final boolean mResizingEnabled;
  private final Producer<EncodedImage> mInputProducer;

  public ResizeAndRotateProducer(
      Executor executor,
      PooledByteBufferFactory pooledByteBufferFactory,
      boolean resizingEnabled,
      Producer<EncodedImage> inputProducer) {
    mExecutor = Preconditions.checkNotNull(executor);
    mPooledByteBufferFactory = Preconditions.checkNotNull(pooledByteBufferFactory);
    mResizingEnabled = resizingEnabled;
    mInputProducer = Preconditions.checkNotNull(inputProducer);
  }

  @Override
  public void produceResults(
      final Consumer<EncodedImage> consumer,
      final ProducerContext context) {
    mInputProducer.produceResults(new TransformingConsumer(consumer, context), context);
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
          shouldTransform(mProducerContext.getImageRequest(), newResult, mResizingEnabled);
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
      InputStream is = null;
      try {
        int numerator = getScaleNumerator(imageRequest, encodedImage, mResizingEnabled);
        extraMap = getExtraMap(encodedImage, imageRequest, numerator);
        is = encodedImage.getInputStream();
        JpegTranscoder.transcodeJpeg(
            is,
            outputStream,
            getRotationAngle(imageRequest.getRotationOptions(), encodedImage),
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
        Closeables.closeQuietly(is);
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

      String requestedSize;
      if (imageRequest.getResizeOptions() != null) {
        requestedSize =
            imageRequest.getResizeOptions().width + "x" + imageRequest.getResizeOptions().height;
      } else {
        requestedSize = "Unspecified";
      }

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
      boolean resizingEnabled) {
    if (encodedImage == null || encodedImage.getImageFormat() == ImageFormat.UNKNOWN) {
      return TriState.UNSET;
    }
    if (encodedImage.getImageFormat() != ImageFormat.JPEG) {
      return TriState.NO;
    }
    return TriState.valueOf(
        shouldRotate(request.getRotationOptions(), encodedImage) ||
            shouldResize(getScaleNumerator(request, encodedImage, resizingEnabled)));
  }

  @VisibleForTesting static float determineResizeRatio(
      ResizeOptions resizeOptions,
      int width,
      int height) {

    if (resizeOptions == null) {
      return 1.0f;
    }

    final float widthRatio = ((float) resizeOptions.width) / width;
    final float heightRatio = ((float) resizeOptions.height) / height;
    float ratio = Math.max(widthRatio, heightRatio);

    if (width * ratio > resizeOptions.maxBitmapSize) {
      ratio = resizeOptions.maxBitmapSize / width;
    }
    if (height * ratio > resizeOptions.maxBitmapSize) {
      ratio = resizeOptions.maxBitmapSize / height;
    }
    return ratio;
  }

  @VisibleForTesting static int roundNumerator(float maxRatio, float roundUpFraction) {
    return (int) (roundUpFraction + maxRatio * JpegTranscoder.SCALE_DENOMINATOR);
  }

  private static int getScaleNumerator(
      ImageRequest imageRequest,
      EncodedImage encodedImage,
      boolean resizingEnabled) {
    if (!resizingEnabled) {
      return JpegTranscoder.SCALE_DENOMINATOR;
    }
    final ResizeOptions resizeOptions = imageRequest.getResizeOptions();
    if (resizeOptions == null) {
      return JpegTranscoder.SCALE_DENOMINATOR;
    }

    final int rotationAngle = getRotationAngle(imageRequest.getRotationOptions(), encodedImage);
    final boolean swapDimensions = rotationAngle == 90 || rotationAngle == 270;
    final int widthAfterRotation = swapDimensions ? encodedImage.getHeight() :
            encodedImage.getWidth();
    final int heightAfterRotation = swapDimensions ? encodedImage.getWidth() :
            encodedImage.getHeight();

    float ratio = determineResizeRatio(resizeOptions, widthAfterRotation, heightAfterRotation);
    int numerator = roundNumerator(ratio, resizeOptions.roundUpFraction);
    if (numerator > MAX_JPEG_SCALE_NUMERATOR) {
      return MAX_JPEG_SCALE_NUMERATOR;
    }
    return (numerator < 1) ? 1 : numerator;
  }

  private static int getRotationAngle(RotationOptions rotationOptions, EncodedImage encodedImage) {
    if (rotationOptions.useImageMetadata()) {
      int rotationAngle = encodedImage.getRotationAngle();
      Preconditions.checkArgument(rotationAngle == 0 || rotationAngle == 90
          || rotationAngle == 180 || rotationAngle == 270);
      return rotationAngle;
    }
    return rotationOptions.getForcedAngle();
  }

  private static boolean shouldResize(int numerator) {
    return numerator < MAX_JPEG_SCALE_NUMERATOR;
  }

  private static boolean shouldRotate(RotationOptions rotationOptions, EncodedImage encodedImage) {
    return !rotationOptions.canDeferUntilRendered() &&
        getRotationAngle(rotationOptions, encodedImage) != 0;
  }
}

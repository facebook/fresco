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
import javax.annotation.concurrent.GuardedBy;

import java.util.Map;
import java.util.concurrent.Executor;

import android.graphics.Bitmap;

import com.facebook.common.internal.ImmutableMap;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.util.UriUtil;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.decoder.ImageDecoder;
import com.facebook.imagepipeline.decoder.ProgressiveJpegConfig;
import com.facebook.imagepipeline.decoder.ProgressiveJpegParser;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.CloseableStaticBitmap;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.image.ImmutableQualityInfo;
import com.facebook.imagepipeline.image.QualityInfo;
import com.facebook.imagepipeline.memory.ByteArrayPool;
import com.facebook.imagepipeline.request.ImageRequest;

import static com.facebook.imagepipeline.producers.JobScheduler.JobRunnable;

/**
 * Decodes images.
 *
 * <p/> Progressive JPEGs are decoded progressively as new data arrives.
 */
public class DecodeProducer implements Producer<CloseableReference<CloseableImage>> {

  public static final String PRODUCER_NAME = "DecodeProducer";

  // keys for extra map
  private static final String BITMAP_SIZE_KEY = "bitmapSize";
  private static final String HAS_GOOD_QUALITY_KEY = "hasGoodQuality";
  private static final String IS_FINAL_KEY = "isFinal";

  private final ByteArrayPool mByteArrayPool;
  private final Executor mExecutor;
  private final ImageDecoder mImageDecoder;
  private final ProgressiveJpegConfig mProgressiveJpegConfig;
  private final Producer<EncodedImage> mNextProducer;
  private final boolean mDownsampleEnabled;

  public DecodeProducer(
      final ByteArrayPool byteArrayPool,
      final Executor executor,
      final ImageDecoder imageDecoder,
      final ProgressiveJpegConfig progressiveJpegConfig,
      final boolean downsampleEnabled,
      final Producer<EncodedImage> nextProducer) {
    mByteArrayPool = Preconditions.checkNotNull(byteArrayPool);
    mExecutor = Preconditions.checkNotNull(executor);
    mImageDecoder = Preconditions.checkNotNull(imageDecoder);
    mProgressiveJpegConfig = Preconditions.checkNotNull(progressiveJpegConfig);
    mDownsampleEnabled = downsampleEnabled;
    mNextProducer = Preconditions.checkNotNull(nextProducer);
  }

  @Override
  public void produceResults(
      final Consumer<CloseableReference<CloseableImage>> consumer,
      final ProducerContext producerContext) {
    final ImageRequest imageRequest = producerContext.getImageRequest();
    ProgressiveDecoder progressiveDecoder;
    if (!UriUtil.isNetworkUri(imageRequest.getSourceUri())) {
      progressiveDecoder = new LocalImagesProgressiveDecoder(consumer, producerContext);
    } else {
      ProgressiveJpegParser jpegParser = new ProgressiveJpegParser(mByteArrayPool);
      progressiveDecoder = new NetworkImagesProgressiveDecoder(
          consumer,
          producerContext,
          jpegParser,
          mProgressiveJpegConfig);
    }
    mNextProducer.produceResults(progressiveDecoder, producerContext);
  }

  private abstract class ProgressiveDecoder extends DelegatingConsumer<
      EncodedImage, CloseableReference<CloseableImage>> {

    private final ProducerContext mProducerContext;
    private final ProducerListener mProducerListener;
    private final ImageDecodeOptions mImageDecodeOptions;

    @GuardedBy("this")
    private boolean mIsFinished;

    private final JobScheduler mJobScheduler;

    public ProgressiveDecoder(
        final Consumer<CloseableReference<CloseableImage>> consumer,
        final ProducerContext producerContext) {
      super(consumer);
      mProducerContext = producerContext;
      mProducerListener = producerContext.getListener();
      mImageDecodeOptions = producerContext.getImageRequest().getImageDecodeOptions();
      mIsFinished = false;
      JobRunnable job = new JobRunnable() {
        @Override
        public void run(EncodedImage encodedImage, boolean isLast) {
          if (encodedImage != null) {
            if (mDownsampleEnabled) {
              encodedImage.setSampleSize(DownsampleUtil.determineSampleSize(
                  producerContext.getImageRequest(), encodedImage));
            }
            doDecode(encodedImage, isLast);
          }
        }
      };
      mJobScheduler = new JobScheduler(mExecutor, job, mImageDecodeOptions.minDecodeIntervalMs);
      mProducerContext.addCallbacks(
          new BaseProducerContextCallbacks() {
            @Override
            public void onIsIntermediateResultExpectedChanged() {
              if (mProducerContext.isIntermediateResultExpected()) {
                mJobScheduler.scheduleJob();
              }
            }
          });
    }

    @Override
    public void onNewResultImpl(EncodedImage newResult, boolean isLast) {
      if (!updateDecodeJob(newResult, isLast)) {
        return;
      }
      if (isLast || mProducerContext.isIntermediateResultExpected()) {
        mJobScheduler.scheduleJob();
      }
    }

    @Override
    public void onFailureImpl(Throwable t) {
      handleError(t);
    }

    @Override
    public void onCancellationImpl() {
      handleCancellation();
    }

    /** Updates the decode job. */
    protected boolean updateDecodeJob(EncodedImage ref, boolean isLast) {
      return mJobScheduler.updateJob(ref, isLast);
    }

    /** Performs the decode synchronously. */
    private void doDecode(EncodedImage encodedImage, boolean isLast) {
      if (isFinished() || !EncodedImage.isValid(encodedImage)) {
        return;
      }

      try {
        long queueTime = mJobScheduler.getQueuedTime();
        int length = isLast ?
            encodedImage.getSize() : getIntermediateImageEndOffset(encodedImage);
        QualityInfo quality = isLast ? ImmutableQualityInfo.FULL_QUALITY : getQualityInfo();

        mProducerListener.onProducerStart(mProducerContext.getId(), PRODUCER_NAME);
        CloseableImage image = null;
        try {
          image = mImageDecoder.decodeImage(encodedImage, length, quality, mImageDecodeOptions);
        } catch (Exception e) {
          Map<String, String> extraMap = getExtraMap(image, queueTime, quality, isLast);
          mProducerListener.
              onProducerFinishWithFailure(mProducerContext.getId(), PRODUCER_NAME, e, extraMap);
          handleError(e);
          return;
        }
        Map<String, String> extraMap = getExtraMap(image, queueTime, quality, isLast);
        mProducerListener.
            onProducerFinishWithSuccess(mProducerContext.getId(), PRODUCER_NAME, extraMap);
        handleResult(image, isLast);
      } finally {
        EncodedImage.closeSafely(encodedImage);
      }
    }

    private Map<String, String> getExtraMap(
        @Nullable CloseableImage image,
        long queueTime,
        QualityInfo quality,
        boolean isFinal) {
      if (!mProducerListener.requiresExtraMap(mProducerContext.getId())) {
        return null;
      }
      String queueStr = String.valueOf(queueTime);
      String qualityStr = String.valueOf(quality.isOfGoodEnoughQuality());
      String finalStr = String.valueOf(isFinal);
      if (image instanceof CloseableStaticBitmap) {
        Bitmap bitmap = ((CloseableStaticBitmap) image).getUnderlyingBitmap();
        String sizeStr = bitmap.getWidth() + "x" + bitmap.getHeight();
        return ImmutableMap.of(
            BITMAP_SIZE_KEY,
            sizeStr,
            JobScheduler.QUEUE_TIME_KEY,
            queueStr,
            HAS_GOOD_QUALITY_KEY,
            qualityStr,
            IS_FINAL_KEY,
            finalStr);
      } else {
        return ImmutableMap.of(
            JobScheduler.QUEUE_TIME_KEY,
            queueStr,
            HAS_GOOD_QUALITY_KEY,
            qualityStr,
            IS_FINAL_KEY,
            finalStr);
      }
    }

    /**
     * @return true if producer is finished
     */
    private synchronized boolean isFinished() {
      return mIsFinished;
    }

    /**
     * Finishes if not already finished and <code>shouldFinish</code> is specified.
     * <p> If just finished, the intermediate image gets released.
     */
    private void maybeFinish(boolean shouldFinish) {
      synchronized (ProgressiveDecoder.this) {
        if (!shouldFinish || mIsFinished) {
          return;
        }
        mIsFinished = true;
      }
      mJobScheduler.clearJob();
    }

    /**
     * Notifies consumer of new result and finishes if the result is final.
     */
    private void handleResult(final CloseableImage decodedImage, final boolean isFinal) {
      CloseableReference<CloseableImage> decodedImageRef = CloseableReference.of(decodedImage);
      try {
        maybeFinish(isFinal);
        getConsumer().onNewResult(decodedImageRef, isFinal);
      } finally {
        CloseableReference.closeSafely(decodedImageRef);
      }
    }

    /**
     * Notifies consumer about the failure and finishes.
     */
    private void handleError(Throwable t) {
      maybeFinish(true);
      getConsumer().onFailure(t);
    }

    /**
     * Notifies consumer about the cancellation and finishes.
     */
    private void handleCancellation() {
      maybeFinish(true);
      getConsumer().onCancellation();
    }

    protected abstract int getIntermediateImageEndOffset(EncodedImage encodedImage);

    protected abstract QualityInfo getQualityInfo();
  }

  private class LocalImagesProgressiveDecoder extends ProgressiveDecoder {

    public LocalImagesProgressiveDecoder(
        final Consumer<CloseableReference<CloseableImage>> consumer,
        final ProducerContext producerContext) {
      super(consumer, producerContext);
    }

    @Override
    protected synchronized boolean updateDecodeJob(EncodedImage encodedImage, boolean isLast) {
      if (!isLast) {
        return false;
      }
      return super.updateDecodeJob(encodedImage, isLast);
    }

    @Override
    protected int getIntermediateImageEndOffset(EncodedImage encodedImage) {
      return encodedImage.getSize();
    }

    @Override
    protected QualityInfo getQualityInfo() {
      return ImmutableQualityInfo.of(0, false, false);
    }
  }

  private class NetworkImagesProgressiveDecoder extends ProgressiveDecoder {

    private final ProgressiveJpegParser mProgressiveJpegParser;
    private final ProgressiveJpegConfig mProgressiveJpegConfig;
    private int mLastScheduledScanNumber;

    public NetworkImagesProgressiveDecoder(
        final Consumer<CloseableReference<CloseableImage>> consumer,
        final ProducerContext producerContext,
        final ProgressiveJpegParser progressiveJpegParser,
        final ProgressiveJpegConfig progressiveJpegConfig) {
      super(consumer, producerContext);
      mProgressiveJpegParser = Preconditions.checkNotNull(progressiveJpegParser);
      mProgressiveJpegConfig = Preconditions.checkNotNull(progressiveJpegConfig);
      mLastScheduledScanNumber = 0;
    }

    @Override
    protected synchronized boolean updateDecodeJob(EncodedImage encodedImage, boolean isLast) {
      boolean ret = super.updateDecodeJob(encodedImage, isLast);
      if (!isLast && EncodedImage.isValid(encodedImage)) {
        if (!mProgressiveJpegParser.parseMoreData(encodedImage)) {
          return false;
        }
        int scanNum = mProgressiveJpegParser.getBestScanNumber();
        if (scanNum <= mLastScheduledScanNumber ||
            scanNum < mProgressiveJpegConfig.getNextScanNumberToDecode(
                mLastScheduledScanNumber)) {
          return false;
        }
        mLastScheduledScanNumber = scanNum;
      }
      return ret;
    }

    @Override
    protected int getIntermediateImageEndOffset(EncodedImage encodedImage) {
      return mProgressiveJpegParser.getBestScanEndOffset();
    }

    @Override
    protected QualityInfo getQualityInfo() {
      return mProgressiveJpegConfig.getQualityInfo(mProgressiveJpegParser.getBestScanNumber());
    }
  }
}

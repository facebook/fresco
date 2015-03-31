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
import java.util.concurrent.TimeUnit;

import android.os.SystemClock;

import com.facebook.common.executors.UiThreadExecutorService;
import com.facebook.common.internal.ImmutableMap;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.util.UriUtil;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.decoder.ProgressiveJpegConfig;
import com.facebook.imagepipeline.decoder.ImageDecoder;
import com.facebook.imagepipeline.decoder.ProgressiveJpegParser;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.ImmutableQualityInfo;
import com.facebook.imagepipeline.image.QualityInfo;
import com.facebook.imagepipeline.memory.ByteArrayPool;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imageformat.ImageFormat;

/**
 * Decodes images.
 *
 * <p/> Progressive JPEGs are decoded progressively as new data arrives.
 */
public class DecodeProducer implements Producer<CloseableReference<CloseableImage>> {

  public static final String PRODUCER_NAME = "DecodeProducer";

  // keys for extra map
  private static final String QUEUE_TIME_KEY = "queueTime";
  private static final String HAS_GOOD_QUALITY_KEY = "hasGoodQuality";
  private static final String IS_FINAL_KEY = "isFinal";

  private final ByteArrayPool mByteArrayPool;
  private final Executor mExecutor;
  private final ImageDecoder mImageDecoder;
  private final ProgressiveJpegConfig mProgressiveJpegConfig;
  private final Producer<CloseableReference<PooledByteBuffer>> mNextProducer;

  public DecodeProducer(
      final ByteArrayPool byteArrayPool,
      final Executor executor,
      final ImageDecoder imageDecoder,
      final ProgressiveJpegConfig progressiveJpegConfig,
      final Producer<CloseableReference<PooledByteBuffer>> nextProducer) {
    mByteArrayPool = Preconditions.checkNotNull(byteArrayPool);
    mExecutor = Preconditions.checkNotNull(executor);
    mImageDecoder = Preconditions.checkNotNull(imageDecoder);
    mProgressiveJpegConfig = Preconditions.checkNotNull(progressiveJpegConfig);
    mNextProducer = Preconditions.checkNotNull(nextProducer);
  }

  @Override
  public void produceResults(
      final Consumer<CloseableReference<CloseableImage>> consumer,
      final ProducerContext context) {
    final ImageRequest imageRequest = context.getImageRequest();
    ProgressiveDecoder progressiveDecoder;
    if (!UriUtil.isNetworkUri(imageRequest.getSourceUri())) {
      progressiveDecoder = new LocalImagesProgressiveDecoder(consumer, context);
    } else {
      ProgressiveJpegParser jpegParser = new ProgressiveJpegParser(mByteArrayPool);
      progressiveDecoder = new NetworkImagesProgressiveDecoder(
          consumer,
          context,
          jpegParser,
          mProgressiveJpegConfig);
    }
    mNextProducer.produceResults(progressiveDecoder, context);
  }

  @VisibleForTesting
  abstract class ProgressiveDecoder extends DelegatingConsumer<
      CloseableReference<PooledByteBuffer>,
      CloseableReference<CloseableImage>> {

    protected final ProducerContext mProducerContext;
    private final ProducerListener mProducerListener;
    private final ImageDecodeOptions mImageDecodeOptions;

    private final Runnable mSubmitDecodeRunnable;

    @GuardedBy("this")
    private boolean mIsFinished;

    // This class is responsible for closing old, non-null reference, and for storing the reference
    // to the latest data. One thing to note is that the reference is overtaken in doDecode().
    // Right before decode happens, reference is cloned (to be held during the decode), and then
    // released (so that we don't issue another decode of the same image). The cloned reference gets
    // released after decode finishes. As a slight optimization, instead of cloning and releasing,
    // reference is just moved.
    @GuardedBy("this")
    @VisibleForTesting CloseableReference<PooledByteBuffer> mImageBytesRef;
    @GuardedBy("this")
    private boolean mIsLast;
    @GuardedBy("this")
    private boolean mIsDecodeSubmitted;
    @GuardedBy("this")
    private long mLastDecodeTime;

    public ProgressiveDecoder(
        final Consumer<CloseableReference<CloseableImage>> consumer,
        final ProducerContext producerContext) {
      super(consumer);
      mProducerContext = producerContext;
      mProducerListener = producerContext.getListener();
      mImageDecodeOptions = producerContext.getImageRequest().getImageDecodeOptions();
      mIsFinished = false;
      mProducerContext.addCallbacks(
          new BaseProducerContextCallbacks() {
            @Override
            public void onIsIntermediateResultExpectedChanged() {
              if (mProducerContext.isIntermediateResultExpected()) {
                scheduleDecodeJob(mImageDecodeOptions.minDecodeIntervalMs);
              }
            }
          });
      mSubmitDecodeRunnable = new Runnable() {
        @Override
        public void run() {
          submitDecode();
        }
      };
    }

    @Override
    public void onNewResultImpl(CloseableReference<PooledByteBuffer> newResult, boolean isLast) {
      if (!updateDecodeJob(newResult, isLast)) {
        return;
      }
      if (isLast || mProducerContext.isIntermediateResultExpected()) {
        scheduleDecodeJob(isLast ? 0 : mImageDecodeOptions.minDecodeIntervalMs);
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
    protected synchronized boolean updateDecodeJob(
        CloseableReference<PooledByteBuffer> imageBytesRef,
        boolean isLast) {
      // ignore invalid intermediate results (should not happen ever, but being defensive)
      if (!isLast && !CloseableReference.isValid(imageBytesRef)) {
        return false;
      }
      CloseableReference.closeSafely(mImageBytesRef);
      mImageBytesRef = CloseableReference.cloneOrNull(imageBytesRef);
      mIsLast = isLast;
      return true;
    }

    /** Schedules the decode, but no sooner than minDecodeIntervalMs since the last decode. */
    private synchronized void scheduleDecodeJob(int minDecodeIntervalMs) {
      if (!mIsDecodeSubmitted) {
        mIsDecodeSubmitted = true;
        long now = SystemClock.uptimeMillis();
        long when = Math.max(mLastDecodeTime + minDecodeIntervalMs, now);
        if (when > now) {
          UiThreadExecutorService.getInstance()
              .schedule(mSubmitDecodeRunnable, when - now, TimeUnit.MILLISECONDS);
        } else {
          mSubmitDecodeRunnable.run();
        }
      }
    }

    /** Submits the decode to the executor. */
    protected void submitDecode() {
      final long submitTime = SystemClock.uptimeMillis();
      mExecutor.execute(
          new Runnable() {
            @Override
            public void run() {
              final long queueTime = SystemClock.uptimeMillis() - submitTime;
              doDecode(queueTime);
            }
          });
    }

    /** Performs the decode synchronously. */
    private void doDecode(long queueTime) {
      CloseableReference<PooledByteBuffer> bytesRef;
      boolean isLast;
      synchronized (ProgressiveDecoder.this) {
        bytesRef = mImageBytesRef;
        mImageBytesRef = null;
        isLast = mIsLast;
        mIsDecodeSubmitted = false;
        mLastDecodeTime = SystemClock.uptimeMillis();
      }

      try {
        if (isFinished() || !CloseableReference.isValid(bytesRef)) {
          return;
        }

        ImageFormat format = isLast ? ImageFormat.UNKNOWN : getImageFormat(bytesRef);
        int length = isLast ? bytesRef.get().size() : getIntermediateImageEndOffset(bytesRef);
        QualityInfo quality = isLast ? ImmutableQualityInfo.FULL_QUALITY : getQualityInfo(bytesRef);

        mProducerListener.onProducerStart(mProducerContext.getId(), PRODUCER_NAME);
        CloseableImage decodedImage;
        try {
          decodedImage =
              mImageDecoder.decodeImage(bytesRef, format, length, quality, mImageDecodeOptions);
        } catch (Exception e) {
          Map<String, String> extraMap = getExtraMap(queueTime, quality, isLast);
          mProducerListener.
              onProducerFinishWithFailure(mProducerContext.getId(), PRODUCER_NAME, e, extraMap);
          handleError(e);
          return;
        }
        Map<String, String> extraMap = getExtraMap(queueTime, quality, isLast);
        mProducerListener.
            onProducerFinishWithSuccess(mProducerContext.getId(), PRODUCER_NAME, extraMap);
        handleResult(decodedImage, isLast);
      } finally {
        CloseableReference.closeSafely(bytesRef);
      }
    }

    private Map<String, String> getExtraMap(
        final long queueTime,
        final QualityInfo qualityInfo,
        final boolean isFinal) {
      if (!mProducerListener.requiresExtraMap(mProducerContext.getId())) {
        return null;
      }
      return ImmutableMap.of(
          QUEUE_TIME_KEY,
          String.valueOf(queueTime),
          HAS_GOOD_QUALITY_KEY,
          String.valueOf(qualityInfo.isOfGoodEnoughQuality()),
          IS_FINAL_KEY,
          String.valueOf(isFinal));
    }

    /**
     * @return true if producer is finished
     */
    private synchronized boolean isFinished() {
      return mIsFinished;
    }

    /**
     * Finishes if not already finished and {@code finish} is specified.
     * <p> If just finished, the intermediate image gets released.
     */
    private synchronized void maybeFinish(boolean finish) {
      if (mIsFinished) {
        return;
      }
      mIsFinished = finish;
      if (finish) {
        CloseableReference.closeSafely(mImageBytesRef);
        mImageBytesRef = null;
      }
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

    /**
     * All these abstract methods are thread-safe.
     */
    @Nullable protected abstract ImageFormat getImageFormat(
        CloseableReference<PooledByteBuffer> imageBytesRef);

    protected abstract int getIntermediateImageEndOffset(
        CloseableReference<PooledByteBuffer> imageBytesRef);

    protected abstract QualityInfo getQualityInfo(
        CloseableReference<PooledByteBuffer> imageBytesRef);
  }

  class LocalImagesProgressiveDecoder extends ProgressiveDecoder {

    public LocalImagesProgressiveDecoder(
        final Consumer<CloseableReference<CloseableImage>> consumer,
        final ProducerContext producerContext) {
      super(consumer, producerContext);
    }

    @Override
    @Nullable protected ImageFormat getImageFormat(
        CloseableReference<PooledByteBuffer> imageBytesRef) {
      return null;
    }

    @Override
    protected int getIntermediateImageEndOffset(
        CloseableReference<PooledByteBuffer> imageBytesRef) {
      return imageBytesRef.get().size();
    }

    @Override
    protected QualityInfo getQualityInfo(CloseableReference<PooledByteBuffer> imageBytesRef) {
      return ImmutableQualityInfo.of(0, false, false);
    }
  }

  class NetworkImagesProgressiveDecoder extends ProgressiveDecoder {
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
    protected synchronized boolean updateDecodeJob(
        CloseableReference<PooledByteBuffer> imageBytesRef,
        boolean isLast) {
      boolean ret = super.updateDecodeJob(imageBytesRef, isLast);
      if (!isLast && CloseableReference.isValid(imageBytesRef)) {
        if (!mProgressiveJpegParser.parseMoreData(imageBytesRef)) {
          return false;
        }
        int scanNum = mProgressiveJpegParser.getBestScanNumber();
        if (scanNum <= mLastScheduledScanNumber ||
            scanNum < mProgressiveJpegConfig.getNextScanNumberToDecode(mLastScheduledScanNumber)) {
          return false;
        }
        mLastScheduledScanNumber = scanNum;
      }
      return ret;
    }

    @Override
    @Nullable protected ImageFormat getImageFormat(
        CloseableReference<PooledByteBuffer> imageBytesRef) {
      return mProgressiveJpegParser.isJpeg() ? ImageFormat.JPEG : ImageFormat.UNKNOWN;
    }

    @Override
    protected int getIntermediateImageEndOffset(
        CloseableReference<PooledByteBuffer> imageBytesRef) {
      return mProgressiveJpegParser.getBestScanEndOffset();
    }

    @Override
    protected QualityInfo getQualityInfo(CloseableReference<PooledByteBuffer> imageBytesRef) {
      return mProgressiveJpegConfig.getQualityInfo(mProgressiveJpegParser.getBestScanNumber());
    }
  }
}

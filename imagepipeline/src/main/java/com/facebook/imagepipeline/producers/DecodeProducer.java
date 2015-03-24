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

import android.os.SystemClock;

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
 * Decodes images. Progressive JPEGs are decoded progressively as new data arrives
 *
 * TODO 5416926: use more elaborate algorithm for throttling decoded scans
 */
public class DecodeProducer implements Producer<CloseableReference<CloseableImage>> {

  @VisibleForTesting
  static final String PRODUCER_NAME = "DecodeProducer";

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
  abstract class ProgressiveDecoder extends BaseConsumer<CloseableReference<PooledByteBuffer>> {

    private final Consumer<CloseableReference<CloseableImage>> mConsumer;
    protected final ProducerContext mProducerContext;
    private final ProducerListener mProducerListener;
    private final ImageDecodeOptions mImageDecodeOptions;

    @GuardedBy("this")
    private boolean mIsFinished;

    @GuardedBy("ProgressiveDecoder.this")
    @VisibleForTesting CloseableReference<PooledByteBuffer> mStoredIntermediateImageBytesRef;
    @GuardedBy("ProgressiveDecoder.this")
    @VisibleForTesting int mStoredIntermediateImageBestScanEnd;
    @GuardedBy("ProgressiveDecoder.this")
    @VisibleForTesting ImageFormat mStoredIntermediateImageFormat;
    @GuardedBy("ProgressiveDecoder.this")
    @VisibleForTesting QualityInfo mStoredIntermediateImageQualityInfo;

    public ProgressiveDecoder(
        final Consumer<CloseableReference<CloseableImage>> consumer,
        final ProducerContext producerContext) {
      mConsumer = consumer;
      mProducerContext = producerContext;
      mProducerListener = producerContext.getListener();
      mImageDecodeOptions = producerContext.getImageRequest().getImageDecodeOptions();
      mIsFinished = false;
      mProducerContext.addCallbacks(
          new BaseProducerContextCallbacks() {
            @Override
            public void onIsIntermediateResultExpectedChanged() {
              if (mProducerContext.isIntermediateResultExpected()) {
                maybeScheduleStoredIntermediateImageDecode();
              }
            }
          });
    }

    private synchronized void maybeScheduleStoredIntermediateImageDecode() {
      if (mStoredIntermediateImageBytesRef != null) {
        scheduleImageDecode(
            mStoredIntermediateImageBytesRef,
            mStoredIntermediateImageBestScanEnd,
            mStoredIntermediateImageFormat,
            mStoredIntermediateImageQualityInfo,
            false);
        closeStoredIntermediateImageBytes();
      }
    }

    protected synchronized void updateStoredIntermediateImage(
        CloseableReference<PooledByteBuffer> intermediateImageBytesRef,
        int intermediateImageBestScanEnd,
        ImageFormat intermediateImageFormat,
        QualityInfo intermediateImageQualityInfo) {
      closeStoredIntermediateImageBytes();
      mStoredIntermediateImageBytesRef = intermediateImageBytesRef.clone();
      mStoredIntermediateImageBestScanEnd = intermediateImageBestScanEnd;
      mStoredIntermediateImageFormat = intermediateImageFormat;
      mStoredIntermediateImageQualityInfo = intermediateImageQualityInfo;
    }

    protected synchronized void closeStoredIntermediateImageBytes() {
      if (mStoredIntermediateImageBytesRef != null) {
        mStoredIntermediateImageBytesRef.close();
        mStoredIntermediateImageBytesRef = null;
      }
    }

    @Override
    public void onNewResultImpl(CloseableReference<PooledByteBuffer> newResult, boolean isLast) {
      if (isLast) {
        decodeFinalImage(newResult);
      } else {
        maybeDecodeIntermediateImage(newResult);
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

    private void decodeFinalImage(final CloseableReference<PooledByteBuffer> imageBytesRef) {
      closeStoredIntermediateImageBytes();
      if (imageBytesRef == null) {
        handleResult(null, true);
      } else {
        scheduleImageDecode(
            imageBytesRef,
            imageBytesRef.get().size(),
            getImageFormat(imageBytesRef),
            ImmutableQualityInfo.FULL_QUALITY,
            /* isFinal */ true);
      }
    }

    protected void maybeDecodeIntermediateImage(
        CloseableReference<PooledByteBuffer> imageBytesRef) {
      Preconditions.checkNotNull(imageBytesRef);
      final int bestScanEnd = getIntermediateImageEndOffset(imageBytesRef);
      final ImageFormat imageFormat = getImageFormat(imageBytesRef);
      final QualityInfo qualityInfo = getQualityInfo(imageBytesRef);

      synchronized (ProgressiveDecoder.this) {
        if (!mProducerContext.isIntermediateResultExpected()) {
          // do not schedule decode as the result is not expected.
          // however, keep the result in case the client should need it in the future.
          updateStoredIntermediateImage(
              imageBytesRef,
              bestScanEnd,
              imageFormat,
              qualityInfo);
        } else {
          closeStoredIntermediateImageBytes();
          scheduleImageDecode(
              imageBytesRef,
              bestScanEnd,
              imageFormat,
              qualityInfo,
              false);
        }
      }
    }

    protected void scheduleImageDecode(
        final CloseableReference<PooledByteBuffer> imageBytesRef,
        final int length,
        @Nullable final ImageFormat imageFormat,
        final QualityInfo qualityInfo,
        final boolean isFinal) {
      final CloseableReference<PooledByteBuffer> imageBytesRefCopy = imageBytesRef.clone();
      final long startTime = SystemClock.elapsedRealtime();
      mExecutor.execute(
          new Runnable() {
            @Override
            public void run() {
              final long queueTime = SystemClock.elapsedRealtime() - startTime;
              try {
                if (isFinished()) {
                  return;
                }
                mProducerListener.onProducerStart(mProducerContext.getId(), PRODUCER_NAME);
                CloseableImage decodedImage = mImageDecoder.decodeImage(
                    imageBytesRefCopy,
                    imageFormat,
                    length,
                    qualityInfo,
                    mImageDecodeOptions);
                mProducerListener.onProducerFinishWithSuccess(
                    mProducerContext.getId(),
                    PRODUCER_NAME,
                    getExtraMap(queueTime, qualityInfo, isFinal));
                handleResult(decodedImage, isFinal);
              } catch (Exception e) {
                mProducerListener.onProducerFinishWithFailure(
                    mProducerContext.getId(),
                    PRODUCER_NAME,
                    e,
                    getExtraMap(queueTime, qualityInfo, isFinal));
                handleError(e);
              } finally {
                imageBytesRefCopy.close();
              }
            }
          });
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
        closeStoredIntermediateImageBytes();
      }
    }

    /**
     * Notifies consumer of new result and finishes if the result is final.
     */
    private void handleResult(final CloseableImage decodedImage, final boolean isFinal) {
      CloseableReference<CloseableImage> decodedImageRef = CloseableReference.of(decodedImage);
      try {
        maybeFinish(isFinal);
        mConsumer.onNewResult(decodedImageRef, isFinal);
      } finally {
        CloseableReference.closeSafely(decodedImageRef);
      }
    }

    /**
     * Notifies consumer about the failure and finishes.
     */
    private void handleError(Throwable t) {
      maybeFinish(true);
      mConsumer.onFailure(t);
    }

    /**
     * Notifies consumer about the cancellation and finishes.
     */
    private void handleCancellation() {
      maybeFinish(true);
      mConsumer.onCancellation();
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

    @VisibleForTesting LocalImagesProgressiveDecoder(
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
    private int mLastDecodedScanNumber;

    NetworkImagesProgressiveDecoder(
        final Consumer<CloseableReference<CloseableImage>> consumer,
        final ProducerContext producerContext,
        final ProgressiveJpegParser progressiveJpegParser,
        final ProgressiveJpegConfig progressiveJpegConfig) {
      super(consumer, producerContext);
      mProgressiveJpegParser = Preconditions.checkNotNull(progressiveJpegParser);
      mProgressiveJpegConfig = Preconditions.checkNotNull(progressiveJpegConfig);
      mLastDecodedScanNumber = 0;
    }

    @Override
    protected void maybeDecodeIntermediateImage(
        CloseableReference<PooledByteBuffer> imageBytesRef) {
      Preconditions.checkNotNull(imageBytesRef);
      if (mProgressiveJpegParser.parseMoreData(imageBytesRef) &&
          mProgressiveJpegParser.getBestScanNumber() >=
              mProgressiveJpegConfig.getNextScanNumberToDecode(mLastDecodedScanNumber)) {
        mLastDecodedScanNumber = mProgressiveJpegParser.getBestScanNumber();
        super.maybeDecodeIntermediateImage(imageBytesRef);
      }
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

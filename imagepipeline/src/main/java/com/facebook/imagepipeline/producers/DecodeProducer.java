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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import android.graphics.Bitmap;

import com.facebook.common.internal.ImmutableMap;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.memory.ByteArrayPool;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.util.ExceptionWithNoStacktrace;
import com.facebook.common.util.UriUtil;
import com.facebook.imageformat.DefaultImageFormats;
import com.facebook.imageformat.ImageFormat;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.decoder.ImageDecoder;
import com.facebook.imagepipeline.decoder.ProgressiveJpegConfig;
import com.facebook.imagepipeline.decoder.ProgressiveJpegParser;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.CloseableStaticBitmap;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.image.ImmutableQualityInfo;
import com.facebook.imagepipeline.image.QualityInfo;
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
  public static final String EXTRA_BITMAP_SIZE = ProducerConstants.EXTRA_BITMAP_SIZE;
  public static final String EXTRA_HAS_GOOD_QUALITY = ProducerConstants.EXTRA_HAS_GOOD_QUALITY;
  public static final String EXTRA_IS_FINAL = ProducerConstants.EXTRA_IS_FINAL;
  public static final String EXTRA_IMAGE_FORMAT_NAME = ProducerConstants.EXTRA_IMAGE_FORMAT_NAME;
  public static final String ENCODED_IMAGE_SIZE = ProducerConstants.ENCODED_IMAGE_SIZE;
  public static final String REQUESTED_IMAGE_SIZE = ProducerConstants.REQUESTED_IMAGE_SIZE;
  public static final String SAMPLE_SIZE = ProducerConstants.SAMPLE_SIZE;

  private final ByteArrayPool mByteArrayPool;
  private final Executor mExecutor;
  private final ImageDecoder mImageDecoder;
  private final ProgressiveJpegConfig mProgressiveJpegConfig;
  private final Producer<EncodedImage> mInputProducer;
  private final boolean mDownsampleEnabled;
  private final boolean mDownsampleEnabledForNetwork;
  private final boolean mDecodeCancellationEnabled;

  public DecodeProducer(
      final ByteArrayPool byteArrayPool,
      final Executor executor,
      final ImageDecoder imageDecoder,
      final ProgressiveJpegConfig progressiveJpegConfig,
      final boolean downsampleEnabled,
      final boolean downsampleEnabledForNetwork,
      final boolean decodeCancellationEnabled,
      final Producer<EncodedImage> inputProducer) {
    mByteArrayPool = Preconditions.checkNotNull(byteArrayPool);
    mExecutor = Preconditions.checkNotNull(executor);
    mImageDecoder = Preconditions.checkNotNull(imageDecoder);
    mProgressiveJpegConfig = Preconditions.checkNotNull(progressiveJpegConfig);
    mDownsampleEnabled = downsampleEnabled;
    mDownsampleEnabledForNetwork = downsampleEnabledForNetwork;
    mInputProducer = Preconditions.checkNotNull(inputProducer);
    mDecodeCancellationEnabled = decodeCancellationEnabled;
  }

  @Override
  public void produceResults(
      final Consumer<CloseableReference<CloseableImage>> consumer,
      final ProducerContext producerContext) {
    final ImageRequest imageRequest = producerContext.getImageRequest();
    ProgressiveDecoder progressiveDecoder;
    if (!UriUtil.isNetworkUri(imageRequest.getSourceUri())) {
      progressiveDecoder = new LocalImagesProgressiveDecoder(
          consumer,
          producerContext,
          mDecodeCancellationEnabled);
    } else {
      ProgressiveJpegParser jpegParser = new ProgressiveJpegParser(mByteArrayPool);
      progressiveDecoder = new NetworkImagesProgressiveDecoder(
          consumer,
          producerContext,
          jpegParser,
          mProgressiveJpegConfig,
          mDecodeCancellationEnabled);
    }
    mInputProducer.produceResults(progressiveDecoder, producerContext);
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
        final ProducerContext producerContext,
        final boolean decodeCancellationEnabled) {
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
              ImageRequest request = producerContext.getImageRequest();
              if (mDownsampleEnabledForNetwork ||
                  !UriUtil.isNetworkUri(request.getSourceUri())) {
                encodedImage.setSampleSize(DownsampleUtil.determineSampleSize(
                    request, encodedImage));
              }
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

            @Override
            public void onCancellationRequested() {
              if (decodeCancellationEnabled) {
                handleCancellation();
              }
            }
          });
    }

    @Override
    public void onNewResultImpl(EncodedImage newResult, boolean isLast) {
      if (isLast && !EncodedImage.isValid(newResult)) {
        handleError(new ExceptionWithNoStacktrace("Encoded image is not valid."));
        return;
      }
      if (!updateDecodeJob(newResult, isLast)) {
        return;
      }
      if (isLast || mProducerContext.isIntermediateResultExpected()) {
        mJobScheduler.scheduleJob();
      }
    }

    @Override
    protected void onProgressUpdateImpl(float progress) {
      super.onProgressUpdateImpl(progress * 0.99f);
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
      final String imageFormatStr;
      ImageFormat imageFormat = encodedImage.getImageFormat();
      if (imageFormat != null) {
        imageFormatStr = imageFormat.getName();
      } else {
        imageFormatStr = "unknown";
      }
      final String encodedImageSize;
      final String sampleSize;
      if (encodedImage != null) {
        encodedImageSize = encodedImage.getWidth() + "x" + encodedImage.getHeight();
        sampleSize = String.valueOf(encodedImage.getSampleSize());
      } else {
        // We should never be here
        encodedImageSize = "unknown";
        sampleSize = "unknown";
      }
      final String requestedSizeStr;
      final ResizeOptions resizeOptions = mProducerContext.getImageRequest().getResizeOptions();
      if (resizeOptions != null) {
        requestedSizeStr = resizeOptions.width + "x" + resizeOptions.height;
      } else {
        requestedSizeStr = "unknown";
      }
      try {
        long queueTime = mJobScheduler.getQueuedTime();
        int length = isLast ?
            encodedImage.getSize() : getIntermediateImageEndOffset(encodedImage);
        QualityInfo quality = isLast ? ImmutableQualityInfo.FULL_QUALITY : getQualityInfo();

        mProducerListener.onProducerStart(mProducerContext.getId(), PRODUCER_NAME);
        CloseableImage image = null;
        try {
          image = mImageDecoder.decode(encodedImage, length, quality, mImageDecodeOptions);
        } catch (Exception e) {
          Map<String, String> extraMap = getExtraMap(
              image,
              queueTime,
              quality,
              isLast,
              imageFormatStr,
              encodedImageSize,
              requestedSizeStr,
              sampleSize);
          mProducerListener.
              onProducerFinishWithFailure(mProducerContext.getId(), PRODUCER_NAME, e, extraMap);
          handleError(e);
          return;
        }
        Map<String, String> extraMap = getExtraMap(
            image,
            queueTime,
            quality,
            isLast,
            imageFormatStr,
            encodedImageSize,
            requestedSizeStr,
            sampleSize);
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
        boolean isFinal,
        String imageFormatName,
        String encodedImageSize,
        String requestImageSize,
        String sampleSize) {
      if (!mProducerListener.requiresExtraMap(mProducerContext.getId())) {
        return null;
      }
      String queueStr = String.valueOf(queueTime);
      String qualityStr = String.valueOf(quality.isOfGoodEnoughQuality());
      String finalStr = String.valueOf(isFinal);
      if (image instanceof CloseableStaticBitmap) {
        Bitmap bitmap = ((CloseableStaticBitmap) image).getUnderlyingBitmap();
        String sizeStr = bitmap.getWidth() + "x" + bitmap.getHeight();
        // We need this because the copyOf() utility method doesn't have a proper overload method
        // for all these parameters
        final Map<String, String> tmpMap = new HashMap<>(8);
        tmpMap.put(EXTRA_BITMAP_SIZE, sizeStr);
        tmpMap.put(JobScheduler.QUEUE_TIME_KEY, queueStr);
        tmpMap.put(EXTRA_HAS_GOOD_QUALITY, qualityStr);
        tmpMap.put(EXTRA_IS_FINAL, finalStr);
        tmpMap.put(ENCODED_IMAGE_SIZE, encodedImageSize);
        tmpMap.put(EXTRA_IMAGE_FORMAT_NAME, imageFormatName);
        tmpMap.put(REQUESTED_IMAGE_SIZE, requestImageSize);
        tmpMap.put(SAMPLE_SIZE, sampleSize);
        return ImmutableMap.copyOf(tmpMap);
      } else {
        final Map<String, String> tmpMap = new HashMap<>(7);
        tmpMap.put(JobScheduler.QUEUE_TIME_KEY, queueStr);
        tmpMap.put(EXTRA_HAS_GOOD_QUALITY, qualityStr);
        tmpMap.put(EXTRA_IS_FINAL, finalStr);
        tmpMap.put(ENCODED_IMAGE_SIZE, encodedImageSize);
        tmpMap.put(EXTRA_IMAGE_FORMAT_NAME, imageFormatName);
        tmpMap.put(REQUESTED_IMAGE_SIZE, requestImageSize);
        tmpMap.put(SAMPLE_SIZE, sampleSize);
        return ImmutableMap.copyOf(tmpMap);
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
        getConsumer().onProgressUpdate(1.0f);
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
        final ProducerContext producerContext,
        final boolean decodeCancellationEnabled) {
      super(consumer, producerContext, decodeCancellationEnabled);
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
        final ProgressiveJpegConfig progressiveJpegConfig,
        final boolean decodeCancellationEnabled) {
      super(consumer, producerContext, decodeCancellationEnabled);
      mProgressiveJpegParser = Preconditions.checkNotNull(progressiveJpegParser);
      mProgressiveJpegConfig = Preconditions.checkNotNull(progressiveJpegConfig);
      mLastScheduledScanNumber = 0;
    }

    @Override
    protected synchronized boolean updateDecodeJob(EncodedImage encodedImage, boolean isLast) {
      boolean ret = super.updateDecodeJob(encodedImage, isLast);
      if (!isLast && EncodedImage.isValid(encodedImage)) {
        if (encodedImage.getImageFormat() != DefaultImageFormats.JPEG) {
          // decoding a partial image data of none jpeg type and displaying that result bitmap
          // to the user have no sense.
          return ret && isNotPartialData(encodedImage);
        }
        if (!mProgressiveJpegParser.parseMoreData(encodedImage)) {
          return false;
        }
        int scanNum = mProgressiveJpegParser.getBestScanNumber();
        if (scanNum <= mLastScheduledScanNumber) {
          // We have already decoded this scan, no need to do so again
          return false;
        }
        if (scanNum < mProgressiveJpegConfig.getNextScanNumberToDecode(mLastScheduledScanNumber)
            && !mProgressiveJpegParser.isEndMarkerRead()) {
          // We have not reached the minimum scan set by the configuration and there
          // are still more scans to be read (the end marker is not reached)
          return false;
        }
        mLastScheduledScanNumber = scanNum;
      }
      return ret;
    }

    private boolean isNotPartialData(EncodedImage encodedImage) {
      return encodedImage.getEncodedCacheKey() != null;
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

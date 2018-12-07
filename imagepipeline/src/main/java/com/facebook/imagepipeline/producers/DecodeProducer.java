/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import static com.facebook.imagepipeline.producers.JobScheduler.JobRunnable;

import android.graphics.Bitmap;
import com.facebook.common.internal.ImmutableMap;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.logging.FLog;
import com.facebook.common.memory.ByteArrayPool;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.util.ExceptionWithNoStacktrace;
import com.facebook.common.util.UriUtil;
import com.facebook.imageformat.DefaultImageFormats;
import com.facebook.imageformat.ImageFormat;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.decoder.DecodeException;
import com.facebook.imagepipeline.decoder.ImageDecoder;
import com.facebook.imagepipeline.decoder.ProgressiveJpegConfig;
import com.facebook.imagepipeline.decoder.ProgressiveJpegParser;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.CloseableStaticBitmap;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.image.ImmutableQualityInfo;
import com.facebook.imagepipeline.image.QualityInfo;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.systrace.FrescoSystrace;
import com.facebook.imagepipeline.transcoder.DownsampleUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

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
  private final int mMaxBitmapSize;

  public DecodeProducer(
      final ByteArrayPool byteArrayPool,
      final Executor executor,
      final ImageDecoder imageDecoder,
      final ProgressiveJpegConfig progressiveJpegConfig,
      final boolean downsampleEnabled,
      final boolean downsampleEnabledForNetwork,
      final boolean decodeCancellationEnabled,
      final Producer<EncodedImage> inputProducer,
      final int maxBitmapSize) {
    mByteArrayPool = Preconditions.checkNotNull(byteArrayPool);
    mExecutor = Preconditions.checkNotNull(executor);
    mImageDecoder = Preconditions.checkNotNull(imageDecoder);
    mProgressiveJpegConfig = Preconditions.checkNotNull(progressiveJpegConfig);
    mDownsampleEnabled = downsampleEnabled;
    mDownsampleEnabledForNetwork = downsampleEnabledForNetwork;
    mInputProducer = Preconditions.checkNotNull(inputProducer);
    mDecodeCancellationEnabled = decodeCancellationEnabled;
    mMaxBitmapSize = maxBitmapSize;
  }

  @Override
  public void produceResults(
      final Consumer<CloseableReference<CloseableImage>> consumer,
      final ProducerContext producerContext) {
    try {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.beginSection("DecodeProducer#produceResults");
      }
      final ImageRequest imageRequest = producerContext.getImageRequest();
      ProgressiveDecoder progressiveDecoder;
      if (!UriUtil.isNetworkUri(imageRequest.getSourceUri())) {
        progressiveDecoder =
            new LocalImagesProgressiveDecoder(
                consumer, producerContext, mDecodeCancellationEnabled, mMaxBitmapSize);
      } else {
        ProgressiveJpegParser jpegParser = new ProgressiveJpegParser(mByteArrayPool);
        progressiveDecoder =
            new NetworkImagesProgressiveDecoder(
                consumer,
                producerContext,
                jpegParser,
                mProgressiveJpegConfig,
                mDecodeCancellationEnabled,
                mMaxBitmapSize);
      }
      mInputProducer.produceResults(progressiveDecoder, producerContext);
    } finally {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
  }

  private abstract class ProgressiveDecoder extends DelegatingConsumer<
      EncodedImage, CloseableReference<CloseableImage>> {

    private final String TAG = "ProgressiveDecoder";
    private static final int DECODE_EXCEPTION_MESSAGE_NUM_HEADER_BYTES = 10;

    private final ProducerContext mProducerContext;
    private final ProducerListener mProducerListener;
    private final ImageDecodeOptions mImageDecodeOptions;

    @GuardedBy("this")
    private boolean mIsFinished;

    private final JobScheduler mJobScheduler;

    public ProgressiveDecoder(
        final Consumer<CloseableReference<CloseableImage>> consumer,
        final ProducerContext producerContext,
        final boolean decodeCancellationEnabled,
        final int maxBitmapSize) {
      super(consumer);
      mProducerContext = producerContext;
      mProducerListener = producerContext.getListener();
      mImageDecodeOptions = producerContext.getImageRequest().getImageDecodeOptions();
      mIsFinished = false;
      JobRunnable job =
          new JobRunnable() {
            @Override
            public void run(EncodedImage encodedImage, @Status int status) {
              if (encodedImage != null) {
                if (mDownsampleEnabled || !statusHasFlag(status, Consumer.IS_RESIZING_DONE)) {
                  ImageRequest request = producerContext.getImageRequest();
                  if (mDownsampleEnabledForNetwork
                      || !UriUtil.isNetworkUri(request.getSourceUri())) {
                    encodedImage.setSampleSize(
                        DownsampleUtil.determineSampleSize(
                            request.getRotationOptions(),
                            request.getResizeOptions(),
                            encodedImage,
                            maxBitmapSize));
                  }
                }
                doDecode(encodedImage, status);
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
    public void onNewResultImpl(EncodedImage newResult, @Status int status) {
      try {
        if (FrescoSystrace.isTracing()) {
          FrescoSystrace.beginSection("DecodeProducer#onNewResultImpl");
        }
        final boolean isLast = isLast(status);
        if (isLast && !EncodedImage.isValid(newResult)) {
          handleError(new ExceptionWithNoStacktrace("Encoded image is not valid."));
          return;
        }
        if (!updateDecodeJob(newResult, status)) {
          return;
        }
        final boolean isPlaceholder = statusHasFlag(status, IS_PLACEHOLDER);
        if (isLast || isPlaceholder || mProducerContext.isIntermediateResultExpected()) {
          mJobScheduler.scheduleJob();
        }
      } finally {
        if (FrescoSystrace.isTracing()) {
          FrescoSystrace.endSection();
        }
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
    protected boolean updateDecodeJob(EncodedImage ref, @Status int status) {
      return mJobScheduler.updateJob(ref, status);
    }

    /** Performs the decode synchronously. */
    private void doDecode(EncodedImage encodedImage, @Status int status) {
      // do not run for partial results of anything except JPEG
      if (encodedImage.getImageFormat() != DefaultImageFormats.JPEG && isNotLast(status)) {
        return;
      }

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
      final String encodedImageSize = encodedImage.getWidth() + "x" + encodedImage.getHeight();
      final String sampleSize = String.valueOf(encodedImage.getSampleSize());
      final boolean isLast = isLast(status);
      final boolean isLastAndComplete = isLast && !statusHasFlag(status, IS_PARTIAL_RESULT);
      final boolean isPlaceholder = statusHasFlag(status, IS_PLACEHOLDER);
      final String requestedSizeStr;
      final ResizeOptions resizeOptions = mProducerContext.getImageRequest().getResizeOptions();
      if (resizeOptions != null) {
        requestedSizeStr = resizeOptions.width + "x" + resizeOptions.height;
      } else {
        requestedSizeStr = "unknown";
      }
      try {
        long queueTime = mJobScheduler.getQueuedTime();
        String requestUri = String.valueOf(mProducerContext.getImageRequest().getSourceUri());
        int length = isLastAndComplete || isPlaceholder
            ? encodedImage.getSize()
            : getIntermediateImageEndOffset(encodedImage);
        QualityInfo quality = isLastAndComplete || isPlaceholder
            ? ImmutableQualityInfo.FULL_QUALITY
            : getQualityInfo();

        mProducerListener.onProducerStart(mProducerContext.getId(), PRODUCER_NAME);
        CloseableImage image = null;
        try {
          try {
            image = mImageDecoder.decode(encodedImage, length, quality, mImageDecodeOptions);
          } catch (DecodeException e) {
            EncodedImage failedEncodedImage = e.getEncodedImage();
            FLog.w(
                TAG,
                "%s, {uri: %s, firstEncodedBytes: %s, length: %d}",
                e.getMessage(),
                requestUri,
                failedEncodedImage.getFirstBytesAsHexString(
                    DECODE_EXCEPTION_MESSAGE_NUM_HEADER_BYTES),
                failedEncodedImage.getSize());
            throw e;
          }
          if (encodedImage.getSampleSize() != EncodedImage.DEFAULT_SAMPLE_SIZE) {
            status |= Consumer.IS_RESIZING_DONE;
          }
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
        handleResult(image, status);
      } finally {
        EncodedImage.closeSafely(encodedImage);
      }
    }

    private @Nullable Map<String, String> getExtraMap(
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
    private void handleResult(final CloseableImage decodedImage, final @Status int status) {
      CloseableReference<CloseableImage> decodedImageRef = CloseableReference.of(decodedImage);
      try {
        maybeFinish(isLast(status));
        getConsumer().onNewResult(decodedImageRef, status);
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
        final boolean decodeCancellationEnabled,
        final int maxBitmapSize) {
      super(consumer, producerContext, decodeCancellationEnabled, maxBitmapSize);
    }

    @Override
    protected synchronized boolean updateDecodeJob(EncodedImage encodedImage, @Status int status) {
      if (isNotLast(status)) {
        return false;
      }
      return super.updateDecodeJob(encodedImage, status);
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
        final boolean decodeCancellationEnabled,
        final int maxBitmapSize) {
      super(consumer, producerContext, decodeCancellationEnabled, maxBitmapSize);
      mProgressiveJpegParser = Preconditions.checkNotNull(progressiveJpegParser);
      mProgressiveJpegConfig = Preconditions.checkNotNull(progressiveJpegConfig);
      mLastScheduledScanNumber = 0;
    }

    @Override
    protected synchronized boolean updateDecodeJob(EncodedImage encodedImage, @Status int status) {
      boolean ret = super.updateDecodeJob(encodedImage, status);
      if ((isNotLast(status) || statusHasFlag(status, IS_PARTIAL_RESULT))
          && !statusHasFlag(status, IS_PLACEHOLDER)
          && EncodedImage.isValid(encodedImage)
          && encodedImage.getImageFormat() == DefaultImageFormats.JPEG) {
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

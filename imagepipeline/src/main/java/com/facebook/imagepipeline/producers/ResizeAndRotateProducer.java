/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import android.media.ExifInterface;
import com.facebook.common.internal.Closeables;
import com.facebook.common.internal.ImmutableList;
import com.facebook.common.internal.ImmutableMap;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.memory.PooledByteBufferFactory;
import com.facebook.common.memory.PooledByteBufferOutputStream;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.util.TriState;
import com.facebook.imageformat.DefaultImageFormats;
import com.facebook.imageformat.ImageFormat;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.common.RotationOptions;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.nativecode.JpegTranscoder;
import com.facebook.imagepipeline.request.ImageRequest;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;

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
  private static final String DOWNSAMPLE_ENUMERATOR_KEY = "downsampleEnumerator";
  private static final String SOFTWARE_ENUMERATOR_KEY = "softwareEnumerator";
  private static final String ROTATION_ANGLE_KEY = "rotationAngle";
  private static final String FRACTION_KEY = "Fraction";
  private static final int FULL_ROUND = 360;

  @VisibleForTesting static final int DEFAULT_JPEG_QUALITY = 85;
  @VisibleForTesting static final int MAX_JPEG_SCALE_NUMERATOR = JpegTranscoder.SCALE_DENOMINATOR;
  @VisibleForTesting static final int MIN_TRANSFORM_INTERVAL_MS = 100;

  /*
   * Inverted EXIF orientations in clockwise rotation order. Rotating 90 degrees clockwise gets you
   * the next item in the list
   */
  private static final ImmutableList<Integer> INVERTED_EXIF_ORIENTATIONS =
      ImmutableList.of(
          ExifInterface.ORIENTATION_FLIP_HORIZONTAL,
          ExifInterface.ORIENTATION_TRANSVERSE,
          ExifInterface.ORIENTATION_FLIP_VERTICAL,
          ExifInterface.ORIENTATION_TRANSPOSE);

  private final Executor mExecutor;
  private final PooledByteBufferFactory mPooledByteBufferFactory;
  private final boolean mResizingEnabled;
  private final Producer<EncodedImage> mInputProducer;
  private final boolean mUseDownsamplingRatio;

  public ResizeAndRotateProducer(
      Executor executor,
      PooledByteBufferFactory pooledByteBufferFactory,
      boolean resizingEnabled,
      Producer<EncodedImage> inputProducer,
      boolean useDownsamplingRatio) {
    mExecutor = Preconditions.checkNotNull(executor);
    mPooledByteBufferFactory = Preconditions.checkNotNull(pooledByteBufferFactory);
    mResizingEnabled = resizingEnabled;
    mInputProducer = Preconditions.checkNotNull(inputProducer);
    mUseDownsamplingRatio = useDownsamplingRatio;
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
        public void run(EncodedImage encodedImage, @Status int status) {
          doTransform(encodedImage, status);
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
    protected void onNewResultImpl(@Nullable EncodedImage newResult, @Status int status) {
      if (mIsCancelled) {
        return;
      }
      boolean isLast = isLast(status);
      if (newResult == null) {
        if (isLast) {
          getConsumer().onNewResult(null, Consumer.IS_LAST);
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
        if (!mProducerContext.getImageRequest().getRotationOptions().canDeferUntilRendered() &&
            newResult.getRotationAngle() != 0 &&
            newResult.getRotationAngle() != EncodedImage.UNKNOWN_ROTATION_ANGLE) {
          newResult = moveImage(newResult); // for thread-safety sake
          newResult.setRotationAngle(0);
        }
        getConsumer().onNewResult(newResult, status);
        return;
      }
      // we know that the result should be transformed, hence schedule it
      if (!mJobScheduler.updateJob(newResult, status)) {
        return;
      }
      if (isLast || mProducerContext.isIntermediateResultExpected()) {
        mJobScheduler.scheduleJob();
      }
    }

    private EncodedImage moveImage(EncodedImage newResult) {
      EncodedImage cloned = EncodedImage.cloneOrNull(newResult);
      newResult.close();
      return cloned;
    }

    private void doTransform(EncodedImage encodedImage, @Status int status) {
      mProducerContext.getListener().onProducerStart(mProducerContext.getId(), PRODUCER_NAME);
      ImageRequest imageRequest = mProducerContext.getImageRequest();
      PooledByteBufferOutputStream outputStream = mPooledByteBufferFactory.newOutputStream();
      Map<String, String> extraMap = null;
      EncodedImage ret = null;
      InputStream is = null;
      try {
        final int softwareNumerator = getSoftwareNumerator(
            imageRequest,
            encodedImage,
            mResizingEnabled);
        final int downsampleRatio = DownsampleUtil.determineSampleSize(imageRequest, encodedImage);
        final int downsampleNumerator = calculateDownsampleNumerator(downsampleRatio);
        final int numerator;
        if (mUseDownsamplingRatio) {
          numerator = downsampleNumerator;
        } else {
          numerator = softwareNumerator;
        }
        is = encodedImage.getInputStream();
        if (INVERTED_EXIF_ORIENTATIONS.contains(encodedImage.getExifOrientation())) {
          // Use exif orientation to rotate since we can't use the rotation angle for
          // inverted exif orientations
          final int exifOrientation =
              getForceRotatedInvertedExifOrientation(
                  imageRequest.getRotationOptions(), encodedImage);
          extraMap =
              getExtraMap(
                  encodedImage, imageRequest, numerator, downsampleNumerator, softwareNumerator, 0);
          JpegTranscoder.transcodeJpegWithExifOrientation(
              is, outputStream, exifOrientation, numerator, DEFAULT_JPEG_QUALITY);
        } else {
          // Use actual rotation angle in degrees to rotate
          final int rotationAngle =
              getRotationAngle(imageRequest.getRotationOptions(), encodedImage);
          extraMap =
              getExtraMap(
                  encodedImage,
                  imageRequest,
                  numerator,
                  downsampleNumerator,
                  softwareNumerator,
                  rotationAngle);
          JpegTranscoder.transcodeJpeg(
              is, outputStream, rotationAngle, numerator, DEFAULT_JPEG_QUALITY);
        }
        CloseableReference<PooledByteBuffer> ref =
            CloseableReference.of(outputStream.toByteBuffer());
        try {
          ret = new EncodedImage(ref);
          ret.setImageFormat(DefaultImageFormats.JPEG);
          try {
            ret.parseMetaData();
            mProducerContext.getListener().
                onProducerFinishWithSuccess(mProducerContext.getId(), PRODUCER_NAME, extraMap);
            if (downsampleRatio != DownsampleUtil.DEFAULT_SAMPLE_SIZE) {
              status |= Consumer.IS_RESIZING_DONE;
            }
            getConsumer().onNewResult(ret, status);
          } finally {
            EncodedImage.closeSafely(ret);
          }
        } finally {
          CloseableReference.closeSafely(ref);
        }
      } catch (Exception e) {
        mProducerContext.getListener().
            onProducerFinishWithFailure(mProducerContext.getId(), PRODUCER_NAME, e, extraMap);
        if (isLast(status)) {
          getConsumer().onFailure(e);
        }
        return;
      } finally {
        Closeables.closeQuietly(is);
        outputStream.close();
      }
    }

    private Map<String, String> getExtraMap(
        EncodedImage encodedImage,
        ImageRequest imageRequest,
        int numerator,
        int downsampleNumerator,
        int softwareNumerator,
        int rotationAngle) {
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
      final Map<String, String> map = new HashMap<>();
      map.put(ORIGINAL_SIZE_KEY, originalSize);
      map.put(REQUESTED_SIZE_KEY, requestedSize);
      map.put(FRACTION_KEY, fraction);
      map.put(JobScheduler.QUEUE_TIME_KEY, String.valueOf(mJobScheduler.getQueuedTime()));
      map.put(DOWNSAMPLE_ENUMERATOR_KEY, Integer.toString(downsampleNumerator));
      map.put(SOFTWARE_ENUMERATOR_KEY, Integer.toString(softwareNumerator));
      map.put(ROTATION_ANGLE_KEY, Integer.toString(rotationAngle));
      return ImmutableMap.copyOf(map);
    }
  }

  private static TriState shouldTransform(
      ImageRequest request,
      EncodedImage encodedImage,
      boolean resizingEnabled) {
    if (encodedImage == null || encodedImage.getImageFormat() == ImageFormat.UNKNOWN) {
      return TriState.UNSET;
    }
    if (encodedImage.getImageFormat() != DefaultImageFormats.JPEG) {
      return TriState.NO;
    }
    return TriState.valueOf(
        shouldRotate(request.getRotationOptions(), encodedImage) ||
            shouldResize(getSoftwareNumerator(request, encodedImage, resizingEnabled)));
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

  private static int getSoftwareNumerator(
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
    int exifOrientation = ExifInterface.ORIENTATION_UNDEFINED;
    if (INVERTED_EXIF_ORIENTATIONS.contains(encodedImage.getExifOrientation())) {
      exifOrientation =
          getForceRotatedInvertedExifOrientation(imageRequest.getRotationOptions(), encodedImage);
    }

    final boolean swapDimensions =
        rotationAngle == 90
            || rotationAngle == 270
            || exifOrientation == ExifInterface.ORIENTATION_TRANSPOSE
            || exifOrientation == ExifInterface.ORIENTATION_TRANSVERSE;
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
    if (!rotationOptions.rotationEnabled()) {
      return RotationOptions.NO_ROTATION;
    }
    int rotationFromMetadata = extractOrientationFromMetadata(encodedImage);
    if (rotationOptions.useImageMetadata()) {
      return rotationFromMetadata;
    }
    return (rotationFromMetadata + rotationOptions.getForcedAngle()) % FULL_ROUND;
  }

  /**
   * Get an inverted exif orientation (2, 4, 5, 7) but adjusted to take the force rotation angle
   * into consideration
   *
   * @throws IllegalArgumentException if encoded image passed doesn't have an inverted EXIF
   *     orientation
   */
  private static int getForceRotatedInvertedExifOrientation(
      RotationOptions rotationOptions, EncodedImage encodedImage) {
    int exifOrientation = encodedImage.getExifOrientation();
    int index = INVERTED_EXIF_ORIENTATIONS.indexOf(exifOrientation);
    if (index < 0) {
      throw new IllegalArgumentException("Only accepts inverted exif orientations");
    }
    int forcedAngle = RotationOptions.NO_ROTATION;
    if (!rotationOptions.useImageMetadata()) {
      forcedAngle = rotationOptions.getForcedAngle();
    }
    int timesToRotate = forcedAngle / 90;
    return INVERTED_EXIF_ORIENTATIONS.get(
        (index + timesToRotate) % INVERTED_EXIF_ORIENTATIONS.size());
  }

  private static int extractOrientationFromMetadata(EncodedImage encodedImage) {
    switch (encodedImage.getRotationAngle()) {
      case RotationOptions.ROTATE_90:
      case RotationOptions.ROTATE_180:
      case RotationOptions.ROTATE_270:
        return encodedImage.getRotationAngle();
      default:
        return 0;
    }
  }

  private static boolean shouldResize(int numerator) {
    return numerator < MAX_JPEG_SCALE_NUMERATOR;
  }

  private static boolean shouldRotate(RotationOptions rotationOptions, EncodedImage encodedImage) {
    return !rotationOptions.canDeferUntilRendered()
        && (getRotationAngle(rotationOptions, encodedImage) != 0
            || shouldRotateUsingExifOrientation(rotationOptions, encodedImage));
  }

  private static boolean shouldRotateUsingExifOrientation(
      RotationOptions rotationOptions, EncodedImage encodedImage) {
    if (!rotationOptions.rotationEnabled() || rotationOptions.canDeferUntilRendered()) {
      encodedImage.setExifOrientation(ExifInterface.ORIENTATION_UNDEFINED);
      return false;
    }
    return INVERTED_EXIF_ORIENTATIONS.contains(encodedImage.getExifOrientation());
  }

  /**
   * This method calculate the ratio in case the downsampling was enabled
   * @param downsampleRatio The ratio from downsampling
   * @return The ratio to use for software resize using the downsampling limitation
   */
  @VisibleForTesting static int calculateDownsampleNumerator(int downsampleRatio) {
    return Math.max(1, JpegTranscoder.SCALE_DENOMINATOR / downsampleRatio);
  }
}

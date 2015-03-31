/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import java.util.concurrent.Executor;

import android.util.Pair;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.util.TriState;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.memory.PooledByteBufferInputStream;
import com.facebook.imagepipeline.memory.PooledByteBufferFactory;
import com.facebook.imagepipeline.memory.PooledByteBufferOutputStream;
import com.facebook.imagepipeline.nativecode.JpegTranscoder;
import com.facebook.imagepipeline.request.ImageRequest;

/**
 * Resizes and rotates jpeg image according to exif orientation data.
 *
 * <p> If image is not jpeg then no transformation is applied.
 */
public class ResizeAndRotateProducer extends ImageTransformProducer<
    Pair<CloseableReference<PooledByteBuffer>, ImageTransformMetaData>, ImageTransformMetaData> {
  private static final String PRODUCER_NAME = "ResizeAndRotateProducer";
  @VisibleForTesting static final int DEFAULT_JPEG_QUALITY = 85;
  @VisibleForTesting static final int MAX_JPEG_SCALE_NUMERATOR = JpegTranscoder.SCALE_DENOMINATOR;

  public ResizeAndRotateProducer(
      Executor executor,
      PooledByteBufferFactory pooledByteBufferFactory,
      Producer<Pair<CloseableReference<PooledByteBuffer>, ImageTransformMetaData>> nextProducer) {
    super(executor, pooledByteBufferFactory, nextProducer);
  }

  @Override
  protected TriState shouldTransform(
      final Pair<CloseableReference<PooledByteBuffer>, ImageTransformMetaData> input,
      final ImageRequest imageRequest,
      boolean isLast) {
    ImageTransformMetaData metaData = input.second;
    switch (metaData.getImageFormat()) {
      case JPEG:
        return isLast ?
            TriState.valueOf(
                getRotationAngle(imageRequest, metaData) != 0 ||
                    getScaleNumerator(imageRequest, metaData) != JpegTranscoder.SCALE_DENOMINATOR) :
            TriState.UNSET;
      case UNKNOWN:
        return isLast ? TriState.NO : TriState.UNSET;
      default:
        return TriState.NO;
    }
  }

  @Override
  protected void transform(
      final CloseableReference<PooledByteBuffer> imageRef,
      final PooledByteBufferOutputStream outputStream,
      final ImageRequest imageRequest,
      final ImageTransformMetaData metaData)
      throws Exception {
    JpegTranscoder.transcodeJpeg(
        new PooledByteBufferInputStream(imageRef.get()),
        outputStream,
        getRotationAngle(imageRequest, metaData),
        getScaleNumerator(imageRequest, metaData),
        DEFAULT_JPEG_QUALITY);
  }

  @Override
  protected CloseableReference<PooledByteBuffer> getImageCopy(
      Pair<CloseableReference<PooledByteBuffer>, ImageTransformMetaData> originalResult) {
    return originalResult.first.clone();
  }

  @Override
  protected ImageTransformMetaData getExtraInformation(
      Pair<CloseableReference<PooledByteBuffer>, ImageTransformMetaData> originalResult) {
    return originalResult.second;
  }

  @Override
  protected Pair<CloseableReference<PooledByteBuffer>, ImageTransformMetaData> createReturnValue(
      PooledByteBuffer transformedBytes,
      ImageTransformMetaData metaData) {
    return Pair.create(CloseableReference.of(transformedBytes), metaData);
  }

  @Override
  protected void closeReturnValue(
      Pair<CloseableReference<PooledByteBuffer>, ImageTransformMetaData> returnValue) {
    CloseableReference.closeSafely(returnValue.first);
  }

  @Override
  protected String getProducerName() {
    return PRODUCER_NAME;
  }

  @Override
  protected boolean shouldAllowCancellation() {
    return true;
  }

  private static int getScaleNumerator(
      final ImageRequest imageRequest,
      final ImageTransformMetaData metaData) {
    final ResizeOptions resizeOptions = imageRequest.getResizeOptions();
    if (resizeOptions == null) {
      return JpegTranscoder.SCALE_DENOMINATOR;
    }

    final int rotationAngle = getRotationAngle(imageRequest, metaData);
    final boolean swapDimensions = rotationAngle == 90 || rotationAngle == 270;
    final int widthAfterRotation = swapDimensions ? metaData.getHeight() : metaData.getWidth();
    final int heightAfterRotation = swapDimensions ? metaData.getWidth() : metaData.getHeight();

    final float widthRatio = ((float) resizeOptions.width) / widthAfterRotation;
    final float heightRatio = ((float) resizeOptions.height) / heightAfterRotation;
    final int numerator =
        (int) Math.ceil(Math.max(widthRatio, heightRatio) * JpegTranscoder.SCALE_DENOMINATOR);

    if (numerator > MAX_JPEG_SCALE_NUMERATOR) {
      return JpegTranscoder.SCALE_DENOMINATOR;
    }
    if (numerator < 1) {
      return 1;
    }
    return numerator;
  }

  private static int getRotationAngle(
      final ImageRequest imageRequest,
      final ImageTransformMetaData metaData) {
    if (!imageRequest.getAutoRotateEnabled()) {
      return 0;
    }
    int rotationAngle = metaData.getRotationAngle();
    Preconditions.checkArgument(
        rotationAngle == 0 || rotationAngle == 90 || rotationAngle == 180 || rotationAngle == 270);
    return rotationAngle;
  }
}

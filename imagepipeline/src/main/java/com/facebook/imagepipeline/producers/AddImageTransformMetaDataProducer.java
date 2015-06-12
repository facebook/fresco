/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import android.graphics.Rect;

import com.facebook.common.references.CloseableReference;
import com.facebook.imageformat.ImageFormat;
import com.facebook.imageformat.ImageFormatChecker;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.memory.PooledByteBufferInputStream;
import com.facebook.imageutils.JfifUtil;

/**
 * Add image transform meta data producer
 *
 * <p>Extracts meta data from the results passed down from the next producer, and adds it to the
 * result that it returns to the consumer.
 */
public class AddImageTransformMetaDataProducer implements Producer<EncodedImage> {
  private final Producer<CloseableReference<PooledByteBuffer>> mNextProducer;

  public AddImageTransformMetaDataProducer(
      Producer<CloseableReference<PooledByteBuffer>> nextProducer) {
    mNextProducer = nextProducer;
  }

  @Override
  public void produceResults(Consumer<EncodedImage> consumer, ProducerContext context) {
    mNextProducer.produceResults(new AddImageTransformMetaDataConsumer(consumer), context);
  }

  private static class AddImageTransformMetaDataConsumer extends DelegatingConsumer<
      CloseableReference<PooledByteBuffer>, EncodedImage> {

    private AddImageTransformMetaDataConsumer(Consumer<EncodedImage> consumer) {
      super(consumer);
    }

    @Override
    protected void onNewResultImpl(
        CloseableReference<PooledByteBuffer> newResult, boolean isLast) {
      if (newResult == null) {
        getConsumer().onNewResult(null, isLast);
        return;
      }
      EncodedImage encodedImage = getEncodedImage(newResult);
      try {
        getConsumer().onNewResult(encodedImage, isLast);
      } finally {
        EncodedImage.closeSafely(encodedImage);
      }
    }
  }

  // Gets the encoded image with the dimensions set if that information is available.
  private static EncodedImage getEncodedImage(CloseableReference<PooledByteBuffer> bytesRef) {
    final ImageFormat imageFormat = ImageFormatChecker.getImageFormat_WrapIOException(
        new PooledByteBufferInputStream(bytesRef.get()));
    if (imageFormat == ImageFormat.JPEG) {
      Rect dimensions =
          JfifUtil.getDimensions(new PooledByteBufferInputStream(bytesRef.get()));
      if (dimensions != null) {
        // We don't know for sure that the rotation angle is set at this point. But it might
        // never get set, so let's assume that if we've got the dimensions then we've got the
        // rotation angle, else we'll never propagate intermediate results.
        return new EncodedImage(
            bytesRef,
            imageFormat,
            getRotationAngle(bytesRef),
            dimensions.width(),
            dimensions.height());
      }
    }
    return new EncodedImage(bytesRef, imageFormat);
  }

  // Gets the correction angle based on the image's orientation
  private static int getRotationAngle(final CloseableReference<PooledByteBuffer> inputRef) {
    return JfifUtil.getAutoRotateAngleFromOrientation(
        JfifUtil.getOrientation(new PooledByteBufferInputStream(inputRef.get())));
  }
}

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

import android.graphics.Rect;
import android.util.Pair;

import com.facebook.imageformat.ImageFormat;
import com.facebook.imageformat.ImageFormatChecker;
import com.facebook.imageutils.JfifUtil;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.memory.PooledByteBufferInputStream;

/**
 * Add image transform meta data producer
 *
 * <p>Extracts meta data from the results passed down from the next producer, and adds it to the
 * result that it returns to the consumer.
 */
public class AddImageTransformMetaDataProducer
    implements Producer<Pair<CloseableReference<PooledByteBuffer>, ImageTransformMetaData>> {
  private final Producer<CloseableReference<PooledByteBuffer>> mNextProducer;

  public AddImageTransformMetaDataProducer(
      Producer<CloseableReference<PooledByteBuffer>> nextProducer) {
    mNextProducer = nextProducer;
  }

  @Override
  public void produceResults(
      Consumer<Pair<CloseableReference<PooledByteBuffer>, ImageTransformMetaData>> consumer,
      ProducerContext context) {
    mNextProducer.produceResults(new AddImageTransformMetaDataConsumer(consumer), context);
  }

  private class AddImageTransformMetaDataConsumer extends DelegatingConsumer<
      CloseableReference<PooledByteBuffer>,
      Pair<CloseableReference<PooledByteBuffer>, ImageTransformMetaData>> {
    @Nullable private ImageTransformMetaData mMetaData = null;

    private AddImageTransformMetaDataConsumer(
        Consumer<Pair<CloseableReference<PooledByteBuffer>, ImageTransformMetaData>> consumer) {
      super(consumer);
    }

    @Override
    protected void onNewResultImpl(
        CloseableReference<PooledByteBuffer> newResult, boolean isLast) {
      if (newResult == null) {
        getConsumer().onNewResult(
            Pair.create(newResult, new ImageTransformMetaData.Builder().build()),
            isLast);
        return;
      }

      if (mMetaData == null || mMetaData.getImageFormat() == ImageFormat.UNKNOWN) {
        final ImageFormat imageFormat = ImageFormatChecker.getImageFormat_WrapIOException(
            new PooledByteBufferInputStream(newResult.get()));
        ImageTransformMetaData.Builder builder = new ImageTransformMetaData.Builder();
        builder.setImageFormat(imageFormat);
        boolean metaDataComplete = false;
        if (imageFormat != ImageFormat.JPEG) {
          metaDataComplete = true;
        } else {
          Rect dimensions =
              JfifUtil.getDimensions(new PooledByteBufferInputStream(newResult.get()));
          if (dimensions != null) {
            metaDataComplete = true;
            builder.setWidth(dimensions.width());
            builder.setHeight(dimensions.height());
            // We don't know for sure that the rotation angle is set at this point. But it might
            // never get set, so let's assume that if we've got the dimensions then we've got the
            // rotation angle, else we'll never propagate intermediate results.
            builder.setRotationAngle(getRotationAngle(newResult));
          }
        }
        if (metaDataComplete) {
          mMetaData = builder.build();
        }
      }
      getConsumer().onNewResult(Pair.create(newResult, mMetaData), isLast);
    }
  }

  // Gets the correction angle based on the image's orientation
  private static int getRotationAngle(final CloseableReference<PooledByteBuffer> inputRef) {
    return JfifUtil.getAutoRotateAngleFromOrientation(
        JfifUtil.getOrientation(new PooledByteBufferInputStream(inputRef.get())));
  }
}

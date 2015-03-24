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
import android.util.Pair;

import com.facebook.imageformat.ImageFormat;
import com.facebook.imageformat.ImageFormatChecker;
import com.facebook.imageutils.JfifUtil;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.memory.PooledByteBuffer;

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

  private class AddImageTransformMetaDataConsumer
      extends BaseConsumer<CloseableReference<PooledByteBuffer>> {
    private final ImageTransformMetaData.Builder mMetaDataBuilder;
    private final Consumer<
        Pair<CloseableReference<PooledByteBuffer>, ImageTransformMetaData>> mConsumer;

    private AddImageTransformMetaDataConsumer(
        Consumer<Pair<CloseableReference<PooledByteBuffer>, ImageTransformMetaData>> consumer) {
      mConsumer = consumer;
      mMetaDataBuilder = new ImageTransformMetaData.Builder();
    }

    @Override
    protected void onNewResultImpl(
        CloseableReference<PooledByteBuffer> newResult, boolean isLast) {
      final ImageFormat imageFormat =
          ImageFormatChecker.getImageFormat_WrapIOException(newResult.get().getStream());
      mMetaDataBuilder.reset();
      mMetaDataBuilder.setImageFormat(imageFormat);
      if (imageFormat == ImageFormat.JPEG && isLast) {
        mMetaDataBuilder.setRotationAngle(getRotationAngle(newResult));
        Rect dimensions = JfifUtil.getDimensions(newResult.get().getStream());
        if (dimensions != null) {
          mMetaDataBuilder.setWidth(dimensions.width());
          mMetaDataBuilder.setHeight(dimensions.height());
        }
      }
      mConsumer.onNewResult(Pair.create(newResult, mMetaDataBuilder.build()), isLast);
    }

    @Override
    protected void onFailureImpl(Throwable t) {
      mConsumer.onFailure(t);
    }

    @Override
    protected void onCancellationImpl() {
      mConsumer.onCancellation();
    }

    // Gets the correction angle based on the image's orientation
    private int getRotationAngle(final CloseableReference<PooledByteBuffer> inputRef) {
      return JfifUtil.getAutoRotateAngleFromOrientation(
          JfifUtil.getOrientation(inputRef.get().getStream()));
    }
  }
}

/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import com.facebook.common.internal.Preconditions;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.image.EncodedImage;

/**
 * Producer that will attempt to retrieve a thumbnail from one or more producers.
 *
 * <p>The producer will try to get a result from each producer only if there is a good chance of it
 * being able to produce a sufficient result.
 *
 * <p> If no underlying producer can provide a suitable result, null result is returned to the
 * consumer
 */
public class ThumbnailBranchProducer implements Producer<EncodedImage> {

  private final ThumbnailProducer<EncodedImage>[] mThumbnailProducers;

  public ThumbnailBranchProducer(ThumbnailProducer<EncodedImage>... thumbnailProducers) {
    mThumbnailProducers = Preconditions.checkNotNull(thumbnailProducers);
    Preconditions.checkElementIndex(0, mThumbnailProducers.length);
  }

  @Override
  public void produceResults(
      final Consumer<EncodedImage> consumer,
      final ProducerContext context) {
    if (context.getImageRequest().getResizeOptions() == null) {
      consumer.onNewResult(null, true);
    } else {
      boolean requested = produceResultsFromThumbnailProducer(0, consumer, context);
      if (!requested) {
        consumer.onNewResult(null, true);
      }
    }
  }

  private class ThumbnailConsumer extends DelegatingConsumer<EncodedImage, EncodedImage> {

    private final ProducerContext mProducerContext;
    private final int mProducerIndex;
    private final ResizeOptions mResizeOptions;

    public ThumbnailConsumer(
        final Consumer<EncodedImage> consumer,
        final ProducerContext producerContext, int producerIndex) {
      super(consumer);
      mProducerContext = producerContext;
      mProducerIndex = producerIndex;
      mResizeOptions = mProducerContext.getImageRequest().getResizeOptions();
    }

    @Override
    protected void onNewResultImpl(EncodedImage newResult, boolean isLast) {
      if (newResult != null &&
          (!isLast || ThumbnailSizeChecker.isImageBigEnough(newResult, mResizeOptions))) {
        getConsumer().onNewResult(newResult, isLast);
      } else if (isLast) {
        EncodedImage.closeSafely(newResult);

        boolean fallback = produceResultsFromThumbnailProducer(
            mProducerIndex + 1,
            getConsumer(),
            mProducerContext);

        if (!fallback) {
          getConsumer().onNewResult(null, true);
        }
      }
    }

    @Override
    protected void onFailureImpl(Throwable t) {
      boolean fallback =
          produceResultsFromThumbnailProducer(mProducerIndex + 1, getConsumer(), mProducerContext);

      if (!fallback) {
        getConsumer().onFailure(t);
      }
    }
  }

  private boolean produceResultsFromThumbnailProducer(
      int startIndex,
      Consumer<EncodedImage> consumer,
      ProducerContext context) {
    int producerIndex =
        findFirstProducerForSize(startIndex, context.getImageRequest().getResizeOptions());

    if (producerIndex == -1) {
      return false;
    }

    mThumbnailProducers[producerIndex]
        .produceResults(new ThumbnailConsumer(consumer, context, producerIndex), context);
    return true;
  }

  private int findFirstProducerForSize(int startIndex, ResizeOptions resizeOptions) {
    for (int i = startIndex; i < mThumbnailProducers.length; i++) {
      if (mThumbnailProducers[i].canProvideImageForSize(resizeOptions)) {
        return i;
      }
    }

    return -1;
  }
}

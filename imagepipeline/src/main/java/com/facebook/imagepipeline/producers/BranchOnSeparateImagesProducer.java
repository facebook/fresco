/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.request.ImageRequest;

/**
 * Producer that coordinates fetching two separate images.
 *
 * <p>The first producer is kicked off, and once it has returned all its results, the second
 * producer is kicked off if necessary.
 */
public class BranchOnSeparateImagesProducer
    implements Producer<EncodedImage> {
  private final Producer<EncodedImage> mInputProducer1;
  private final Producer<EncodedImage> mInputProducer2;

  public BranchOnSeparateImagesProducer(
      Producer<EncodedImage> inputProducer1, Producer<EncodedImage> inputProducer2) {
    mInputProducer1 = inputProducer1;
    mInputProducer2 = inputProducer2;
  }

  @Override
  public void produceResults(
      Consumer<EncodedImage> consumer,
      ProducerContext context) {
    OnFirstImageConsumer onFirstImageConsumer = new OnFirstImageConsumer(consumer, context);
    mInputProducer1.produceResults(onFirstImageConsumer, context);
  }

  private class OnFirstImageConsumer extends DelegatingConsumer<EncodedImage, EncodedImage> {

    private ProducerContext mProducerContext;

    private OnFirstImageConsumer(
        Consumer<EncodedImage> consumer,
        ProducerContext producerContext) {
      super(consumer);
      mProducerContext = producerContext;
    }

    @Override
    protected void onNewResultImpl(EncodedImage newResult, boolean isLast) {
      ImageRequest request = mProducerContext.getImageRequest();
      boolean isGoodEnough =
          ThumbnailSizeChecker.isImageBigEnough(newResult, request.getResizeOptions());
      if (newResult != null && (isGoodEnough || request.getLocalThumbnailPreviewsEnabled())) {
        getConsumer().onNewResult(newResult, isLast && isGoodEnough);
      }
      if (isLast && !isGoodEnough) {
        EncodedImage.closeSafely(newResult);

        mInputProducer2.produceResults(getConsumer(), mProducerContext);
      }
    }

    @Override
    protected void onFailureImpl(Throwable t) {
      mInputProducer2.produceResults(getConsumer(), mProducerContext);
    }
  }
}

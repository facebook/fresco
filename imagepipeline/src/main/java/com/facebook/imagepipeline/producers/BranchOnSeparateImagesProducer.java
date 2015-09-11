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
  private final Producer<EncodedImage> mNextProducer1;
  private final Producer<EncodedImage> mNextProducer2;

  public BranchOnSeparateImagesProducer(
      Producer<EncodedImage> nextProducer1, Producer<EncodedImage> nextProducer2) {
    mNextProducer1 = nextProducer1;
    mNextProducer2 = nextProducer2;
  }

  @Override
  public void produceResults(
      Consumer<EncodedImage> consumer,
      ProducerContext context) {
    OnFirstImageConsumer onFirstImageConsumer = new OnFirstImageConsumer(consumer, context);
    mNextProducer1.produceResults(onFirstImageConsumer, context);
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
      boolean isGoodEnough = isResultGoodEnough(newResult, request);
      if (newResult != null && (isGoodEnough || request.getLocalThumbnailPreviewsEnabled())) {
        getConsumer().onNewResult(newResult, isLast && isGoodEnough);
      }
      if (isLast && !isGoodEnough) {
        mNextProducer2.produceResults(getConsumer(), mProducerContext);
      }
    }

    @Override
    protected void onFailureImpl(Throwable t) {
      mNextProducer2.produceResults(getConsumer(), mProducerContext);
    }

    private boolean isResultGoodEnough(EncodedImage encodedImage, ImageRequest imageRequest) {
      if (encodedImage == null) {
        return false;
      }

      return encodedImage.getWidth() >= imageRequest.getPreferredWidth() &&
          encodedImage.getHeight() >= imageRequest.getPreferredHeight();
    }
  }
}

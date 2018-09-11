/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
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
    protected void onNewResultImpl(EncodedImage newResult, @Status int status) {
      ImageRequest request = mProducerContext.getImageRequest();
      boolean isLast = isLast(status);
      boolean isGoodEnough =
          ThumbnailSizeChecker.isImageBigEnough(newResult, request.getResizeOptions());
      if (newResult != null && (isGoodEnough || request.getLocalThumbnailPreviewsEnabled())) {
        if (isLast && isGoodEnough) {
          getConsumer().onNewResult(newResult, status);
        } else {
          int alteredStatus = turnOffStatusFlag(status, IS_LAST);
          getConsumer().onNewResult(newResult, alteredStatus);
        }
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

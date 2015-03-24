/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import android.util.Pair;

import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.request.ImageRequest;

/**
 * Producer that coordinates fetching two separate images.
 *
 * <p>The first producer is kicked off, and once it has returned all its results, the second
 * producer is kicked off if necessary.
 */
public class BranchOnSeparateImagesProducer
    implements Producer<Pair<CloseableReference<PooledByteBuffer>, ImageTransformMetaData>> {
  private final Producer<
      Pair<CloseableReference<PooledByteBuffer>, ImageTransformMetaData>> mNextProducer1;
  private final Producer<
      Pair<CloseableReference<PooledByteBuffer>, ImageTransformMetaData>> mNextProducer2;

  public BranchOnSeparateImagesProducer(
      Producer<Pair<CloseableReference<PooledByteBuffer>, ImageTransformMetaData>> nextProducer1,
      Producer<Pair<CloseableReference<PooledByteBuffer>, ImageTransformMetaData>> nextProducer2) {
    mNextProducer1 = nextProducer1;
    mNextProducer2 = nextProducer2;
  }

  @Override
  public void produceResults(
      Consumer<Pair<CloseableReference<PooledByteBuffer>,
          ImageTransformMetaData>> consumer,
      ProducerContext context) {
    OnFirstImageConsumer onFirstImageConsumer = new OnFirstImageConsumer(consumer, context);
    mNextProducer1.produceResults(onFirstImageConsumer, context);
  }

  private class OnFirstImageConsumer
      extends BaseConsumer<Pair<CloseableReference<PooledByteBuffer>, ImageTransformMetaData>> {

    private Consumer<Pair<CloseableReference<PooledByteBuffer>, ImageTransformMetaData>> mConsumer;
    private ProducerContext mProducerContext;

    private OnFirstImageConsumer(
        Consumer<Pair<CloseableReference<PooledByteBuffer>, ImageTransformMetaData>> consumer,
        ProducerContext producerContext) {
      mConsumer = consumer;
      mProducerContext = producerContext;
    }

    @Override
    protected void onNewResultImpl(
        Pair<CloseableReference<PooledByteBuffer>, ImageTransformMetaData> newResult,
        boolean isLast) {
      boolean isGoodEnough = isResultGoodEnough(newResult, mProducerContext.getImageRequest());
      if (newResult != null &&
          (isGoodEnough || mProducerContext.getImageRequest().getLocalThumbnailPreviewsEnabled())) {
        mConsumer.onNewResult(newResult, isLast && isGoodEnough);
      }
      if (isLast && !isGoodEnough) {
        mNextProducer2.produceResults(mConsumer, mProducerContext);
      }
    }

    @Override
    protected void onFailureImpl(Throwable t) {
      mNextProducer2.produceResults(mConsumer, mProducerContext);
    }

    @Override
    protected void onCancellationImpl() {
      mConsumer.onCancellation();
    }

    private boolean isResultGoodEnough(
        Pair<CloseableReference<PooledByteBuffer>, ImageTransformMetaData> newResult,
        ImageRequest imageRequest) {
      if (newResult == null) {
        return false;
      }

      ImageTransformMetaData metaData = newResult.second;
      return metaData.getWidth() >= imageRequest.getPreferredWidth() &&
          metaData.getHeight() >= imageRequest.getPreferredHeight();
    }
  }
}

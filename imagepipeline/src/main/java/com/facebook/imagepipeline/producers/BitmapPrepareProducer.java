/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import android.graphics.Bitmap;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.CloseableStaticBitmap;

/**
 * This producer issues to a call to {@link android.graphics.Bitmap#prepareToDraw()} to allow the
 * RendererThread upload the bitmap to GPU asynchronously before it is used. This has no affect on
 * Android versions before N.
 *
 * Controlled via {@link com.facebook.imagepipeline.core.ImagePipelineExperiments#mUseBitmapPrepareToDraw}
 */
public class BitmapPrepareProducer implements Producer<CloseableReference<CloseableImage>> {

  public static final String PRODUCER_NAME = "BitmapPrepareProducer";

  private final Producer<CloseableReference<CloseableImage>> mInputProducer;

  public BitmapPrepareProducer(final Producer<CloseableReference<CloseableImage>> inputProducer) {
    mInputProducer = Preconditions.checkNotNull(inputProducer);
  }

  @Override
  public void produceResults(
      final Consumer<CloseableReference<CloseableImage>> consumer,
      final ProducerContext producerContext) {
    if (producerContext.isPrefetch()) {
      // do not prepare bitmaps that are pre-fetcheds
      mInputProducer.produceResults(consumer, producerContext);
    } else {
      mInputProducer.produceResults(new BitmapPrepareConsumer(consumer), producerContext);
    }
  }

  private static class BitmapPrepareConsumer extends
      DelegatingConsumer<CloseableReference<CloseableImage>, CloseableReference<CloseableImage>> {

    BitmapPrepareConsumer(Consumer<CloseableReference<CloseableImage>> consumer) {
      super(consumer);
    }

    @Override
    protected void onNewResultImpl(
        CloseableReference<CloseableImage> newResult,
        @Status int status) {
      internalPrepareBitmap(newResult);
      getConsumer().onNewResult(newResult, status);
    }

    private static void internalPrepareBitmap(CloseableReference<CloseableImage> newResult) {
      if (newResult == null || !newResult.isValid()) {
        return;
      }

      final CloseableImage closeableImage = newResult.get();
      if (closeableImage == null || closeableImage.isClosed()) {
        return;
      }

      if (closeableImage instanceof CloseableStaticBitmap) {
        final CloseableStaticBitmap staticBitmap = (CloseableStaticBitmap) closeableImage;
        final Bitmap bitmap = staticBitmap.getUnderlyingBitmap();
        if (bitmap == null) {
          return;
        }

        bitmap.prepareToDraw();
      }
    }
  }
}

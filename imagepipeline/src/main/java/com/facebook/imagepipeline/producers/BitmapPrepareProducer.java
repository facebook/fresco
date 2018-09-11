/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
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
  private final int mMinBitmapSizeBytes;
  private final int mMaxBitmapSizeBytes;
  private final boolean mPreparePrefetch;

  /**
   * @param inputProducer The next producer in the pipeline
   * @param minBitmapSizeBytes Bitmaps with a {@link Bitmap#getByteCount()} smaller than this value
   *     are not uploaded
   * @param maxBitmapSizeBytes Bitmaps with a {@link Bitmap#getByteCount()} larger than this value
   *     are not uploaded
   */
  public BitmapPrepareProducer(
      final Producer<CloseableReference<CloseableImage>> inputProducer,
      int minBitmapSizeBytes,
      int maxBitmapSizeBytes,
      boolean preparePrefetch) {
    Preconditions.checkArgument(minBitmapSizeBytes <= maxBitmapSizeBytes);
    mInputProducer = Preconditions.checkNotNull(inputProducer);
    mMinBitmapSizeBytes = minBitmapSizeBytes;
    mMaxBitmapSizeBytes = maxBitmapSizeBytes;
    mPreparePrefetch = preparePrefetch;
  }

  @Override
  public void produceResults(
      final Consumer<CloseableReference<CloseableImage>> consumer,
      final ProducerContext producerContext) {
    // only prepare pre-fetched bitmaps if enabled
    if (producerContext.isPrefetch() && !mPreparePrefetch) {
      mInputProducer.produceResults(consumer, producerContext);
    } else {
      mInputProducer.produceResults(
          new BitmapPrepareConsumer(consumer, mMinBitmapSizeBytes, mMaxBitmapSizeBytes),
          producerContext);
    }
  }

  private static class BitmapPrepareConsumer extends
      DelegatingConsumer<CloseableReference<CloseableImage>, CloseableReference<CloseableImage>> {

    private final int mMinBitmapSizeBytes;
    private final int mMaxBitmapSizeBytes;

    BitmapPrepareConsumer(
        Consumer<CloseableReference<CloseableImage>> consumer,
        int minBitmapSizeBytes,
        int maxBitmapSizeBytes) {
      super(consumer);
      mMinBitmapSizeBytes = minBitmapSizeBytes;
      mMaxBitmapSizeBytes = maxBitmapSizeBytes;
    }

    @Override
    protected void onNewResultImpl(
        CloseableReference<CloseableImage> newResult,
        @Status int status) {
      internalPrepareBitmap(newResult);
      getConsumer().onNewResult(newResult, status);
    }

    private void internalPrepareBitmap(CloseableReference<CloseableImage> newResult) {
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

        final int bitmapByteCount = bitmap.getRowBytes() * bitmap.getHeight();
        if (bitmapByteCount < mMinBitmapSizeBytes) {
          return;
        }
        if (bitmapByteCount > mMaxBitmapSizeBytes) {
          return;
        }

        bitmap.prepareToDraw();
      }
    }
  }
}

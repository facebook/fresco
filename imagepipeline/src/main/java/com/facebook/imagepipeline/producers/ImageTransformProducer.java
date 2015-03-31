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

import java.util.concurrent.Executor;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.util.TriState;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.memory.PooledByteBufferFactory;
import com.facebook.imagepipeline.memory.PooledByteBufferOutputStream;
import com.facebook.imagepipeline.request.ImageRequest;

/**
 * Base class for producers that wait for entire image and then transform it in some way:
 * rotate, resize, transcode to different format.
 *
 * <p> Subclasses should provide implementations for shouldTransform and transform methods.
 * The first one is called when producer receives new image data to determine whether any work has
 * to be done on image bytes when entire image is available. Second one is called only if first one
 * returns true and is responsible for doing any required transformation.
 *
 * <p>E is the type of any extra information that the producer requires.
 */
public abstract class ImageTransformProducer<T, E>
    implements Producer<T> {

  private final Executor mExecutor;
  private final PooledByteBufferFactory mPooledByteBufferFactory;
  private final Producer<T> mNextProducer;

  protected ImageTransformProducer(
      Executor executor,
      PooledByteBufferFactory pooledByteBufferFactory,
      Producer<T> nextProducer) {
    mExecutor = Preconditions.checkNotNull(executor);
    mPooledByteBufferFactory = Preconditions.checkNotNull(pooledByteBufferFactory);
    mNextProducer = Preconditions.checkNotNull(nextProducer);
  }

  @Override
  public void produceResults(final Consumer<T> consumer, final ProducerContext context) {
    mNextProducer.produceResults(new TransformingConsumer(consumer, context), context);
  }

  private class TransformingConsumer extends DelegatingConsumer<T, T> {

    private final ProducerContext mContext;
    private TriState mShouldTransformWhenFinished;

    public TransformingConsumer(final Consumer<T> consumer, final ProducerContext context) {
      super(consumer);
      mContext = context;
      mShouldTransformWhenFinished = TriState.UNSET;
    }

    @Override
    protected void onNewResultImpl(@Nullable T newResult, boolean isLast) {
      // try to determine if the last result should be transformed
      if (mShouldTransformWhenFinished == TriState.UNSET && newResult != null) {
        mShouldTransformWhenFinished =
            shouldTransform(newResult, mContext.getImageRequest(), isLast);
      }

      // just propagate result if it shouldn't be transformed
      if (mShouldTransformWhenFinished == TriState.NO) {
        getConsumer().onNewResult(newResult, isLast);
        return;
      }

      if (isLast) {
        if (mShouldTransformWhenFinished == TriState.YES) {
          transformLastResult(newResult, getConsumer(), mContext);
        } else {
          getConsumer().onNewResult(newResult, isLast);
        }
      }
    }
  }

  private void transformLastResult(
      final T originalResult,
      final Consumer<T> consumer,
      final ProducerContext producerContext) {
    final ProducerListener listener = producerContext.getListener();
    final String requestId = producerContext.getId();
    final CloseableReference<PooledByteBuffer> imageRefCopy = getImageCopy(originalResult);
    final E extraInformation = getExtraInformation(originalResult);
    final StatefulProducerRunnable<T> cancellableProducerRunnable =
        new StatefulProducerRunnable<T>(consumer, listener, getProducerName(), requestId) {
          @Override
          protected T getResult() throws Exception {
            ImageRequest imageRequest = producerContext.getImageRequest();
            PooledByteBufferOutputStream outputStream = mPooledByteBufferFactory.newOutputStream();
            try {
              transform(imageRefCopy, outputStream, imageRequest, extraInformation);
              return createReturnValue(outputStream.toByteBuffer(), extraInformation);
            } finally {
              outputStream.close();
            }
          }

          @Override
          protected void disposeResult(T result) {
            closeReturnValue(result);
          }

          @Override
          protected void onSuccess(T result) {
            imageRefCopy.close();
            super.onSuccess(result);
          }

          @Override
          protected void onFailure(Exception e) {
            imageRefCopy.close();
            super.onFailure(e);
          }

          @Override
          protected void onCancellation() {
            imageRefCopy.close();
            super.onCancellation();
          }
        };
    if (shouldAllowCancellation()) {
      producerContext.addCallbacks(
          new BaseProducerContextCallbacks() {
            @Override
            public void onCancellationRequested() {
              cancellableProducerRunnable.cancel();
            }
          });
    }
    mExecutor.execute(cancellableProducerRunnable);
  }

  /**
   * @return YES if encoded image referenced by imageRef needs to be transformed when final result
   * is received. Can be called subsequently, and as soon as it returns result other than UNSET -
   * it becomes final
   */
  protected abstract TriState shouldTransform(
      T input,
      ImageRequest imageRequest,
      boolean isLast);

  /**
   * Transforms image bytes
   *
   * @param imageRef image bytes
   * @param outputStream stream to write transformed image to
   * @param imageRequest image request
   * @param extraData any extra data passed to the producer
   * @throws Exception
   */
  protected abstract void transform(
      CloseableReference<PooledByteBuffer> imageRef,
      PooledByteBufferOutputStream outputStream,
      ImageRequest imageRequest,
      E extraData) throws Exception;

  /**
   * Extracts a copy of the image bytes from the result received from the next producer.
   */
  protected abstract CloseableReference<PooledByteBuffer> getImageCopy(T originalResult);

  /**
   * Extracts any extra data that was received.
   */
  @Nullable protected abstract E getExtraInformation(T originalResult);

  /**
   * Creates a return value to pass to the consumer from the transformed bytes.
   */
  protected abstract T createReturnValue(PooledByteBuffer transformedBytes, E extraInformation);

  /**
   * Closes the return value that was passed to the consumer.
   */
  protected abstract void closeReturnValue(T returnValue);

  /**
   * Gets the name of the producer
   */
  protected abstract String getProducerName();

  /**
   * Should return true if cancellation after transformation has been scheduled is desired
   */
  protected abstract boolean shouldAllowCancellation();
}

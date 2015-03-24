/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;

import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.memory.PooledByteBufferFactory;
import com.facebook.imagepipeline.request.ImageRequest;

/**
 * Represents a local fetch producer.
 */
public abstract class LocalFetchProducer implements Producer<CloseableReference<PooledByteBuffer>> {

  private final Executor mExecutor;
  private final PooledByteBufferFactory mPooledByteBufferFactory;

  protected LocalFetchProducer(
      Executor executor,
      PooledByteBufferFactory pooledByteBufferFactory) {
    mExecutor = executor;
    mPooledByteBufferFactory = pooledByteBufferFactory;
  }

  @Override
  public void produceResults(
      final Consumer<CloseableReference<PooledByteBuffer>> consumer,
      final ProducerContext producerContext) {

    final ProducerListener listener = producerContext.getListener();
    final String requestId = producerContext.getId();
    final ImageRequest imageRequest = producerContext.getImageRequest();
    final StatefulProducerRunnable cancellableProducerRunnable =
        new StatefulProducerRunnable<CloseableReference<PooledByteBuffer>>(
            consumer,
            listener,
            getProducerName(),
            requestId) {

          @Override
          protected CloseableReference<PooledByteBuffer> getResult() throws Exception {
            InputStream inputStream = null;
            try {
              inputStream = getInputStream(imageRequest);
              int length = getLength(imageRequest);
              if (length < 0) {
                return CloseableReference.of(mPooledByteBufferFactory.newByteBuffer(inputStream));
              } else {
                return CloseableReference.of(
                    mPooledByteBufferFactory.newByteBuffer(inputStream, length));
              }
            } finally {
              if (inputStream != null) {
                inputStream.close();
              }
            }
          }

          @Override
          protected void disposeResult(CloseableReference<PooledByteBuffer> result) {
            CloseableReference.closeSafely(result);
          }
        };

    producerContext.addCallbacks(
        new BaseProducerContextCallbacks() {
          @Override
          public void onCancellationRequested() {
            cancellableProducerRunnable.cancel();
          }
        });
    mExecutor.execute(cancellableProducerRunnable);
  }

  /**
   * Gets an input stream from the local resource.
   * @param imageRequest request that includes the local resource that is being accessed
   * @throws IOException
   */
  protected abstract InputStream getInputStream(ImageRequest imageRequest) throws IOException;

  /**
   * Gets the length of the input from the payload.
   * @param imageRequest request that includes the local resource that is being accessed
   * @return length of the input indicated by the payload. -1 indicates that the length is unknown.
   */
  protected abstract int getLength(ImageRequest imageRequest);

  /**
   * @return name of the Producer
   */
  protected abstract String getProducerName();
}

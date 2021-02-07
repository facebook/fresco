/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import com.facebook.common.internal.Closeables;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.memory.PooledByteBufferFactory;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.request.ImageRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;

/** Represents a local fetch producer. */
public abstract class LocalFetchProducer implements Producer<EncodedImage> {

  private final Executor mExecutor;
  private final PooledByteBufferFactory mPooledByteBufferFactory;

  protected LocalFetchProducer(Executor executor, PooledByteBufferFactory pooledByteBufferFactory) {
    mExecutor = executor;
    mPooledByteBufferFactory = pooledByteBufferFactory;
  }

  @Override
  public void produceResults(
      final Consumer<EncodedImage> consumer, final ProducerContext producerContext) {

    final ProducerListener2 listener = producerContext.getProducerListener();
    final ImageRequest imageRequest = producerContext.getImageRequest();
    producerContext.putOriginExtra("local", "fetch");
    final StatefulProducerRunnable cancellableProducerRunnable =
        new StatefulProducerRunnable<EncodedImage>(
            consumer, listener, producerContext, getProducerName()) {

          @Override
          protected @Nullable EncodedImage getResult() throws Exception {
            EncodedImage encodedImage = getEncodedImage(imageRequest);
            if (encodedImage == null) {
              listener.onUltimateProducerReached(producerContext, getProducerName(), false);
              producerContext.putOriginExtra("local");
              return null;
            }
            encodedImage.parseMetaData();
            listener.onUltimateProducerReached(producerContext, getProducerName(), true);
            producerContext.putOriginExtra("local");
            return encodedImage;
          }

          @Override
          protected void disposeResult(EncodedImage result) {
            EncodedImage.closeSafely(result);
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

  /** Creates a memory-backed encoded image from the stream. The stream is closed. */
  protected EncodedImage getByteBufferBackedEncodedImage(InputStream inputStream, int length)
      throws IOException {
    CloseableReference<PooledByteBuffer> ref = null;
    try {
      if (length <= 0) {
        ref = CloseableReference.of(mPooledByteBufferFactory.newByteBuffer(inputStream));
      } else {
        ref = CloseableReference.of(mPooledByteBufferFactory.newByteBuffer(inputStream, length));
      }
      return new EncodedImage(ref);
    } finally {
      Closeables.closeQuietly(inputStream);
      CloseableReference.closeSafely(ref);
    }
  }

  protected EncodedImage getEncodedImage(InputStream inputStream, int length) throws IOException {
    return getByteBufferBackedEncodedImage(inputStream, length);
  }

  /**
   * Gets an encoded image from the local resource. It can be either backed by a FileInputStream or
   * a PooledByteBuffer
   *
   * @param imageRequest request that includes the local resource that is being accessed
   * @throws IOException
   */
  protected abstract EncodedImage getEncodedImage(ImageRequest imageRequest) throws IOException;

  /** @return name of the Producer */
  protected abstract String getProducerName();
}

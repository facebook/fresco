/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;

import com.facebook.common.internal.Supplier;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.memory.PooledByteBufferFactory;
import com.facebook.imagepipeline.request.ImageRequest;

/**
 * Represents a local fetch producer.
 */
public abstract class LocalFetchProducer implements Producer<EncodedImage> {

  private final Executor mExecutor;
  private final PooledByteBufferFactory mPooledByteBufferFactory;
  private final boolean mDownsampleEnabled;

  protected LocalFetchProducer(
      Executor executor,
      PooledByteBufferFactory pooledByteBufferFactory,
      boolean downsampleEnabled) {
    mExecutor = executor;
    mPooledByteBufferFactory = pooledByteBufferFactory;
    mDownsampleEnabled = downsampleEnabled;
  }

  @Override
  public void produceResults(
      final Consumer<EncodedImage> consumer,
      final ProducerContext producerContext) {

    final ProducerListener listener = producerContext.getListener();
    final String requestId = producerContext.getId();
    final ImageRequest imageRequest = producerContext.getImageRequest();
    final StatefulProducerRunnable cancellableProducerRunnable =
        new StatefulProducerRunnable<EncodedImage>(
            consumer,
            listener,
            getProducerName(),
            requestId) {

          @Override
          protected EncodedImage getResult() throws Exception {
            EncodedImage encodedImage = getEncodedImage(imageRequest);
            encodedImage.parseMetaData();
            if (mDownsampleEnabled && EncodedImage.isMetaDataAvailable(encodedImage)) {
              encodedImage.setSampleSize(
                  DownsampleUtil.determineSampleSize(imageRequest, encodedImage));
            }
            // If the image is not going to be downsampled, read it into memory
            if (encodedImage.getSampleSize() == EncodedImage.DEFAULT_SAMPLE_SIZE) {
              CloseableReference<PooledByteBuffer> bytesRef = encodedImage.getByteBufferRef();
              try {
                if (bytesRef == null) {
                  EncodedImage oldEncodedImage = encodedImage;
                  try {
                    encodedImage = getByteBufferBackedEncodedImage(
                        oldEncodedImage.getInputStream(), oldEncodedImage.getSize());
                    encodedImage.copyMetaDataFrom(oldEncodedImage);
                  } finally {
                    EncodedImage.closeSafely(oldEncodedImage);
                  }
                }
              } finally {
                CloseableReference.closeSafely(bytesRef);
              }
            }
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

  protected EncodedImage getByteBufferBackedEncodedImage(
      InputStream inputStream,
      int length) throws IOException {
    CloseableReference<PooledByteBuffer> ref = null;
    try {
      if (length < 0) {
        ref = CloseableReference.of(mPooledByteBufferFactory.newByteBuffer(inputStream));
      } else {
        ref = CloseableReference.of(mPooledByteBufferFactory.newByteBuffer(inputStream, length));
      }
      return new EncodedImage(ref);
    } finally {
      CloseableReference.closeSafely(ref);
    }
  }

  protected EncodedImage getFileBackedEncodedImage(final String pathname, int length) {
    return getFileBackedEncodedImage(new File(pathname), length);
  }

  protected EncodedImage getFileBackedEncodedImage(final File file, int length) {
    Supplier<FileInputStream> sup = new Supplier<FileInputStream>() {
      @Override
      public FileInputStream get() {
        try {
          return new FileInputStream(file);
        } catch (IOException ioe) {
          throw new RuntimeException(ioe);
        }
      }
    };
    return new EncodedImage(sup, length);
  }

  /**
   * Gets an encoded image from the local resource. It can be either backed by a FileInputStream or
   * a PooledByteBuffer
   * @param imageRequest request that includes the local resource that is being accessed
   * @throws IOException
   */
  protected abstract EncodedImage getEncodedImage(ImageRequest imageRequest) throws IOException;

  /**
   * @return name of the Producer
   */
  protected abstract String getProducerName();
}

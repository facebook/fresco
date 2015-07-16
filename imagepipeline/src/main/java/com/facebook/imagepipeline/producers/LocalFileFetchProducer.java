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
import java.util.concurrent.Executor;

import com.facebook.common.internal.VisibleForTesting;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.memory.PooledByteBufferFactory;
import com.facebook.imagepipeline.request.ImageRequest;

/**
 * Represents a local file fetch producer.
 */
public class LocalFileFetchProducer extends LocalFetchProducer {
  @VisibleForTesting static final String PRODUCER_NAME = "LocalFileFetchProducer";

  public LocalFileFetchProducer(
      Executor executor,
      PooledByteBufferFactory pooledByteBufferFactory,
      boolean downsampleEnabled) {
    super(executor, pooledByteBufferFactory, downsampleEnabled);
  }

  @Override
  protected EncodedImage getEncodedImage(final ImageRequest imageRequest) throws IOException {
    return getFileBackedEncodedImage(
        imageRequest.getSourceFile(), (int) imageRequest.getSourceFile().length());
  }

  @Override
  protected String getProducerName() {
    return PRODUCER_NAME;
  }
}

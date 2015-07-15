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

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;

import com.facebook.common.internal.VisibleForTesting;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.memory.PooledByteBufferFactory;
import com.facebook.imagepipeline.request.ImageRequest;

/**
 * Executes a local fetch from an asset.
 */
public class LocalAssetFetchProducer extends LocalFetchProducer {
  @VisibleForTesting static final String PRODUCER_NAME = "LocalAssetFetchProducer";

  private final AssetManager mAssetManager;

  public LocalAssetFetchProducer(
      Executor executor,
      PooledByteBufferFactory pooledByteBufferFactory,
      boolean downsampleEnabled,
      AssetManager assetManager) {
    super(executor, pooledByteBufferFactory, downsampleEnabled);
    mAssetManager = assetManager;
  }

  @Override
  protected EncodedImage getEncodedImage(ImageRequest imageRequest) throws IOException {
    return getByteBufferBackedEncodedImage(
        mAssetManager.open(getAssetName(imageRequest), AssetManager.ACCESS_STREAMING),
        getLength(imageRequest));
  }

  private int getLength(ImageRequest imageRequest) {
    AssetFileDescriptor fd = null;
    try {
      fd = mAssetManager.openFd(getAssetName(imageRequest));
      return (int) fd.getLength();
    } catch (IOException e) {
      return -1;
    } finally {
      try {
        if (fd != null) {
          fd.close();
        }
      } catch (IOException ignored) {
        // There's nothing we can do with the exception when closing descriptor.
      }
    }
  }

  @Override
  protected String getProducerName() {
    return PRODUCER_NAME;
  }

  private static String getAssetName(ImageRequest imageRequest) {
    return imageRequest.getSourceUri().getPath().substring(1);
  }
}

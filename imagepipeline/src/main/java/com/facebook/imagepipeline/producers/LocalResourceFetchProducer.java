/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.memory.PooledByteBufferFactory;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.infer.annotation.Nullsafe;
import java.io.IOException;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;

/** Executes a local fetch from a resource. */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class LocalResourceFetchProducer extends LocalFetchProducer {

  public static final String PRODUCER_NAME = "LocalResourceFetchProducer";

  private final Resources mResources;

  public LocalResourceFetchProducer(
      Executor executor, PooledByteBufferFactory pooledByteBufferFactory, Resources resources) {
    super(executor, pooledByteBufferFactory);
    mResources = resources;
  }

  @Override
  protected @Nullable EncodedImage getEncodedImage(ImageRequest imageRequest) throws IOException {
    return getEncodedImage(
        mResources.openRawResource(getResourceId(imageRequest)), getLength(imageRequest));
  }

  private int getLength(ImageRequest imageRequest) {
    AssetFileDescriptor fd = null;
    try {
      fd = mResources.openRawResourceFd(getResourceId(imageRequest));
      return (int) fd.getLength();
    } catch (Resources.NotFoundException e) {
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

  private static int getResourceId(ImageRequest imageRequest) {
    String path = imageRequest.getSourceUri().getPath();
    Preconditions.checkNotNull(path);
    return Integer.parseInt(path.substring(1));
  }
}

/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import android.content.ContentResolver;
import android.net.Uri;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.memory.PooledByteBufferFactory;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.infer.annotation.Nullsafe;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;

/**
 * The {@link QualifiedResourceFetchProducer} uses the {@link ContentResolver} to allow fetching
 * resources that might not be part of the application's package.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class QualifiedResourceFetchProducer extends LocalFetchProducer {

  public static final String PRODUCER_NAME = "QualifiedResourceFetchProducer";

  private final ContentResolver mContentResolver;

  public QualifiedResourceFetchProducer(
      Executor executor,
      PooledByteBufferFactory pooledByteBufferFactory,
      ContentResolver contentResolver) {
    super(executor, pooledByteBufferFactory);
    mContentResolver = contentResolver;
  }

  @Override
  protected @Nullable EncodedImage getEncodedImage(final ImageRequest imageRequest)
      throws IOException {
    final Uri uri = imageRequest.getSourceUri();
    final InputStream inputStream = mContentResolver.openInputStream(uri);
    Preconditions.checkNotNull(inputStream, "ContentResolver returned null InputStream");

    return getEncodedImage(inputStream, EncodedImage.UNKNOWN_STREAM_SIZE);
  }

  @Override
  protected String getProducerName() {
    return PRODUCER_NAME;
  }
}

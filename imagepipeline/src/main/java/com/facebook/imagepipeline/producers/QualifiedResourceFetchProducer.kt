/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers

import android.content.ContentResolver
import com.facebook.common.memory.PooledByteBufferFactory
import com.facebook.imagepipeline.image.EncodedImage
import com.facebook.imagepipeline.request.ImageRequest
import java.io.IOException
import java.util.concurrent.Executor

/**
 * The [QualifiedResourceFetchProducer] uses the [ContentResolver] to allow fetching resources that
 * might not be part of the application's package.
 */
class QualifiedResourceFetchProducer(
    executor: Executor,
    pooledByteBufferFactory: PooledByteBufferFactory,
    private val contentResolver: ContentResolver
) : LocalFetchProducer(executor, pooledByteBufferFactory) {

  @Throws(IOException::class)
  override fun getEncodedImage(imageRequest: ImageRequest): EncodedImage {
    return getEncodedImage(
        checkNotNull(contentResolver.openInputStream(imageRequest.sourceUri)) {
          "ContentResolver returned null InputStream"
        },
        EncodedImage.UNKNOWN_STREAM_SIZE)
  }

  override fun getProducerName(): String = PRODUCER_NAME

  companion object {
    const val PRODUCER_NAME = "QualifiedResourceFetchProducer"
  }
}

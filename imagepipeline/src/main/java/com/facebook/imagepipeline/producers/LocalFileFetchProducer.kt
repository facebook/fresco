/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers

import com.facebook.common.memory.PooledByteBufferFactory
import com.facebook.imagepipeline.image.EncodedImage
import com.facebook.imagepipeline.request.ImageRequest
import java.io.FileInputStream
import java.io.IOException
import java.util.concurrent.Executor

/** Represents a local file fetch producer. */
class LocalFileFetchProducer(executor: Executor, pooledByteBufferFactory: PooledByteBufferFactory) :
    LocalFetchProducer(executor, pooledByteBufferFactory) {

  @Throws(IOException::class)
  override fun getEncodedImage(imageRequest: ImageRequest): EncodedImage? =
      getEncodedImage(
          FileInputStream(imageRequest.sourceFile.toString()),
          imageRequest.sourceFile.length().toInt())

  override fun getProducerName(): String = PRODUCER_NAME

  companion object {
    const val PRODUCER_NAME = "LocalFileFetchProducer"
  }
}

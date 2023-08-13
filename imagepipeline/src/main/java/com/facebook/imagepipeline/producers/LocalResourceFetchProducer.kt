/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers

import android.content.res.AssetFileDescriptor
import android.content.res.Resources
import android.content.res.Resources.NotFoundException
import com.facebook.common.memory.PooledByteBufferFactory
import com.facebook.imagepipeline.image.EncodedImage
import com.facebook.imagepipeline.request.ImageRequest
import java.io.IOException
import java.util.concurrent.Executor

/** Executes a local fetch from a resource. */
class LocalResourceFetchProducer(
    executor: Executor,
    pooledByteBufferFactory: PooledByteBufferFactory,
    private val resources: Resources
) : LocalFetchProducer(executor, pooledByteBufferFactory) {

  @Throws(IOException::class)
  override fun getEncodedImage(imageRequest: ImageRequest): EncodedImage? =
      getEncodedImage(
          resources.openRawResource(getResourceId(imageRequest)), getLength(imageRequest))

  private fun getLength(imageRequest: ImageRequest): Int {
    var fd: AssetFileDescriptor? = null
    return try {
      fd = resources.openRawResourceFd(getResourceId(imageRequest))
      fd.length.toInt()
    } catch (e: NotFoundException) {
      -1
    } finally {
      try {
        fd?.close()
      } catch (ignored: IOException) {
        // There's nothing we can do with the exception when closing descriptor.
      }
    }
  }

  override fun getProducerName(): String = PRODUCER_NAME

  companion object {
    const val PRODUCER_NAME = "LocalResourceFetchProducer"

    private fun getResourceId(imageRequest: ImageRequest): Int {
      val path = imageRequest.sourceUri.path
      checkNotNull(path)
      return path!!.substring(1).toInt()
    }
  }
}

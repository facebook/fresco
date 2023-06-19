/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers

import android.content.res.AssetFileDescriptor
import android.content.res.AssetManager
import com.facebook.common.memory.PooledByteBufferFactory
import com.facebook.imagepipeline.image.EncodedImage
import com.facebook.imagepipeline.request.ImageRequest
import java.io.IOException
import java.util.concurrent.Executor

/** Executes a local fetch from an asset. */
class LocalAssetFetchProducer(
    executor: Executor,
    pooledByteBufferFactory: PooledByteBufferFactory,
    private val assetManager: AssetManager
) : LocalFetchProducer(executor, pooledByteBufferFactory) {

  @Throws(IOException::class)
  override fun getEncodedImage(imageRequest: ImageRequest): EncodedImage? =
      getEncodedImage(
          assetManager.open(getAssetName(imageRequest), AssetManager.ACCESS_STREAMING),
          getLength(imageRequest))

  private fun getLength(imageRequest: ImageRequest): Int {
    var fd: AssetFileDescriptor? = null
    return try {
      fd = assetManager.openFd(getAssetName(imageRequest))
      fd.length.toInt()
    } catch (e: IOException) {
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
    const val PRODUCER_NAME = "LocalAssetFetchProducer"

    private fun getAssetName(imageRequest: ImageRequest): String =
        imageRequest.sourceUri.path!!.substring(1)
  }
}

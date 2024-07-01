/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers

import com.facebook.imagepipeline.cache.BufferedDiskCache
import com.facebook.imagepipeline.request.ImageRequest
import com.facebook.imagepipeline.request.ImageRequest.CacheChoice

object DiskCacheDecision {

  @JvmStatic
  fun chooseDiskCacheForRequest(
      imageRequest: ImageRequest,
      smallDiskCache: BufferedDiskCache?,
      defaultDiskCache: BufferedDiskCache?,
      dynamicDiskCaches: Map<String, BufferedDiskCache>?
  ): BufferedDiskCache? {
    if (imageRequest.cacheChoice == CacheChoice.SMALL) {
      return smallDiskCache
    }
    if (imageRequest.cacheChoice == CacheChoice.DEFAULT) {
      return defaultDiskCache
    }
    if (imageRequest.cacheChoice == CacheChoice.DYNAMIC && dynamicDiskCaches != null) {
      val diskCacheId = imageRequest.diskCacheId
      if (diskCacheId != null) {
        return dynamicDiskCaches[diskCacheId]
      }
    }
    return null
  }

  internal class DiskCacheDecisionNoDiskCacheChosenException(message: String?) : Exception(message)
}

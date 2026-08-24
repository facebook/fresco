/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache

data class CacheKeyDimensions(val width: Int, val height: Int)

data class UnifiedCacheKeyInput(
    val normalizedUri: NormalizedUri,
    val customCacheKey: String? = null,
    val bitmapDimensions: CacheKeyDimensions? = null,
    val encodedDimensions: CacheKeyDimensions? = bitmapDimensions,
)

data class ResolvedCacheKeyStrings(
    val bitmapSourceKey: String,
    val encodedKey: String,
)

data class UnifiedCacheKeyGeneratorConfig(
    val includeDimensionsInBitmapKey: Boolean = false,
    val includeDimensionsInEncodedKey: Boolean = false,
    val enableDiskSimilarity: Boolean = false,
    val hashThreshold: Int = Int.MAX_VALUE,
    val hashEncodedKey: Boolean = true,
)

/** Deterministic cache-key string generation without image-request framework dependencies. */
class UnifiedCacheKeyGenerator(private val config: UnifiedCacheKeyGeneratorConfig) {

  fun resolve(
      input: UnifiedCacheKeyInput,
      hashThreshold: Int = config.hashThreshold,
  ): ResolvedCacheKeyStrings {
    val customKey = input.customCacheKey
    var bitmapKey = customKey ?: input.normalizedUri.cacheKeyString
    if (customKey == null && config.includeDimensionsInBitmapKey) {
      input.bitmapDimensions?.let { bitmapKey = "$bitmapKey${it.width}_${it.height}" }
    }
    if (customKey == null && bitmapKey.length >= hashThreshold) {
      bitmapKey = bitmapKey.hashCode().toString()
    }

    if (canReuseBitmapSourceKey(input, customKey)) {
      return ResolvedCacheKeyStrings(bitmapSourceKey = bitmapKey, encodedKey = bitmapKey)
    }

    var encodedKey = customKey ?: input.normalizedUri.cacheKeyString
    if (customKey == null && config.includeDimensionsInEncodedKey) {
      input.encodedDimensions?.let {
        encodedKey =
            if (config.enableDiskSimilarity && input.normalizedUri.groupKey != null) {
              "${input.normalizedUri.groupKey}_${it.width}_${it.height}"
            } else {
              "$encodedKey${it.width}_${it.height}"
            }
      }
    }
    if (customKey == null && config.hashEncodedKey && encodedKey.length >= hashThreshold) {
      encodedKey = encodedKey.hashCode().toString()
    }

    return ResolvedCacheKeyStrings(bitmapSourceKey = bitmapKey, encodedKey = encodedKey)
  }

  private fun canReuseBitmapSourceKey(
      input: UnifiedCacheKeyInput,
      customKey: String?,
  ): Boolean =
      customKey != null ||
          (!config.enableDiskSimilarity &&
              config.includeDimensionsInBitmapKey == config.includeDimensionsInEncodedKey &&
              (!config.includeDimensionsInBitmapKey ||
                  input.bitmapDimensions == input.encodedDimensions) &&
              config.hashEncodedKey)
}

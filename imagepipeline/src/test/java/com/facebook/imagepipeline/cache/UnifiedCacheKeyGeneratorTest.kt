/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class UnifiedCacheKeyGeneratorTest {
  @Test
  fun resolve_goldenVectors() {
    assertThat(generator().resolve(input())).isEqualTo(ResolvedCacheKeyStrings("base", "base"))
    assertThat(generator(dimensions = true).resolve(input(CacheKeyDimensions(100, 200))))
        .isEqualTo(ResolvedCacheKeyStrings("base100_200", "base100_200"))
    assertThat(
        generator(dimensions = true, similarity = true).resolve(input(CacheKeyDimensions(-1, -1))),
    )
        .isEqualTo(ResolvedCacheKeyStrings("base-1_-1", "group_-1_-1"))
  }

  @Test
  fun resolve_customKeyBypassesDimensionsAndHashing() {
    val result =
        generator(dimensions = true, threshold = 5)
            .resolve(
                UnifiedCacheKeyInput(
                    normalizedUri = NormalizedUri("ignored"),
                    customCacheKey = "custom-key",
                    bitmapDimensions = CacheKeyDimensions(10, 20),
                ),
            )
    assertThat(result).isEqualTo(ResolvedCacheKeyStrings("custom-key", "custom-key"))
  }

  @Test
  fun resolve_usesPerCallHashThreshold() {
    val result = generator(threshold = Int.MAX_VALUE).resolve(input(), hashThreshold = 1)

    assertThat(result.bitmapSourceKey).isEqualTo("base".hashCode().toString())
    assertThat(result.encodedKey).isEqualTo("base".hashCode().toString())
  }

  @Test
  fun resolve_hashesAtThreshold() {
    val result =
        generator(dimensions = true, threshold = 6).resolve(input(CacheKeyDimensions(1, 2)))
    assertThat(result.bitmapSourceKey).isEqualTo("base1_2".hashCode().toString())
    assertThat(result.encodedKey).isEqualTo("base1_2".hashCode().toString())
  }

  @Test
  fun resolve_reusesBitmapResultWhenPoliciesAndDimensionsAreEquivalent() {
    val result =
        generator(dimensions = true, threshold = 1).resolve(input(CacheKeyDimensions(100, 200)))

    assertThat(result.encodedKey).isSameAs(result.bitmapSourceKey)
  }

  @Test
  fun resolve_doesNotReuseBitmapResultWhenEncodedHashingIsDisabled() {
    val result = UnifiedCacheKeyGenerator(
        UnifiedCacheKeyGeneratorConfig(
            includeDimensionsInBitmapKey = true,
            includeDimensionsInEncodedKey = true,
            hashThreshold = 1,
            hashEncodedKey = false,
        ),
    )
        .resolve(input(CacheKeyDimensions(100, 200)))

    assertThat(result.bitmapSourceKey).isEqualTo("base100_200".hashCode().toString())
    assertThat(result.encodedKey).isEqualTo("base100_200")
  }

  @Test
  fun resolve_preservesIndependentDimensionPoliciesWhenNotEquivalent() {
    val dimensions = CacheKeyDimensions(100, 200)
    val input = input(dimensions)

    assertThat(
        UnifiedCacheKeyGenerator(
            UnifiedCacheKeyGeneratorConfig(
                includeDimensionsInBitmapKey = true,
                includeDimensionsInEncodedKey = false,
            ),
        )
            .resolve(input),
    )
        .isEqualTo(ResolvedCacheKeyStrings("base100_200", "base"))
    assertThat(
        UnifiedCacheKeyGenerator(
            UnifiedCacheKeyGeneratorConfig(
                includeDimensionsInBitmapKey = false,
                includeDimensionsInEncodedKey = true,
            ),
        )
            .resolve(input),
    )
        .isEqualTo(ResolvedCacheKeyStrings("base", "base100_200"))
  }

  @Test
  fun resolve_usesSeparateBitmapAndEncodedDimensions() {
    val result =
        generator(dimensions = true, similarity = true)
            .resolve(
                UnifiedCacheKeyInput(
                    normalizedUri = NormalizedUri("base", "group"),
                    bitmapDimensions = CacheKeyDimensions(320, 480),
                    encodedDimensions = CacheKeyDimensions(10, 20),
                ),
            )

    assertThat(result).isEqualTo(ResolvedCacheKeyStrings("base320_480", "group_10_20"))
  }

  private fun input(dimensions: CacheKeyDimensions? = null) =
      UnifiedCacheKeyInput(NormalizedUri("base", "group"), bitmapDimensions = dimensions)

  private fun generator(
      dimensions: Boolean = false,
      similarity: Boolean = false,
      threshold: Int = Int.MAX_VALUE,
  ) = UnifiedCacheKeyGenerator(
      UnifiedCacheKeyGeneratorConfig(dimensions, dimensions, similarity, threshold),
  )
}

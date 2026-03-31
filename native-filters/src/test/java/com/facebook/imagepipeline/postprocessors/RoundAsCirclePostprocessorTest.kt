/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.postprocessors

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class RoundAsCirclePostprocessorTest {

  @Test
  fun testDefaultConstructor() {
    val postProcessor = RoundAsCirclePostprocessor()
    assertThat(postProcessor).isNotNull()
  }

  @Test
  fun testConstructorWithAntiAliasing() {
    val postProcessor = RoundAsCirclePostprocessor(true)
    assertThat(postProcessor).isNotNull()
  }

  @Test
  fun testConstructorWithoutAntiAliasing() {
    val postProcessor = RoundAsCirclePostprocessor(false)
    assertThat(postProcessor).isNotNull()
  }

  @Test
  fun testGetPostprocessorCacheKeyWithAntiAliasing() {
    val postProcessor = RoundAsCirclePostprocessor(true)
    val cacheKey = postProcessor.postprocessorCacheKey
    assertThat(cacheKey).isNotNull()
    assertThat(cacheKey.toString()).contains("AntiAliased")
  }

  @Test
  fun testGetPostprocessorCacheKeyWithoutAntiAliasing() {
    val postProcessor = RoundAsCirclePostprocessor(false)
    val cacheKey = postProcessor.postprocessorCacheKey
    assertThat(cacheKey).isNotNull()
    assertThat(cacheKey.toString()).doesNotContain("AntiAliased")
  }

  @Test
  fun testCacheKeyIsCached() {
    val postProcessor = RoundAsCirclePostprocessor()
    val cacheKey1 = postProcessor.postprocessorCacheKey
    val cacheKey2 = postProcessor.postprocessorCacheKey
    assertThat(cacheKey1).isSameAs(cacheKey2)
  }

  @Test
  fun testDifferentAntiAliasingSettingsProduceDifferentCacheKeys() {
    val postProcessor1 = RoundAsCirclePostprocessor(true)
    val postProcessor2 = RoundAsCirclePostprocessor(false)
    val cacheKey1 = postProcessor1.postprocessorCacheKey
    val cacheKey2 = postProcessor2.postprocessorCacheKey
    assertThat(cacheKey1).isNotEqualTo(cacheKey2)
  }
}

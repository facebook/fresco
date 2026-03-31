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
class IterativeBoxBlurPostProcessorTest {

  @Test
  fun testConstructorWithValidParams() {
    val postProcessor = IterativeBoxBlurPostProcessor(3, 10)
    assertThat(postProcessor).isNotNull()
  }

  @Test
  fun testConstructorWithSingleParam() {
    val postProcessor = IterativeBoxBlurPostProcessor(10)
    assertThat(postProcessor).isNotNull()
  }

  @Test(expected = IllegalArgumentException::class)
  fun testConstructorWithZeroIterations() {
    IterativeBoxBlurPostProcessor(0, 10)
  }

  @Test(expected = IllegalArgumentException::class)
  fun testConstructorWithNegativeIterations() {
    IterativeBoxBlurPostProcessor(-1, 10)
  }

  @Test(expected = IllegalArgumentException::class)
  fun testConstructorWithZeroRadius() {
    IterativeBoxBlurPostProcessor(3, 0)
  }

  @Test(expected = IllegalArgumentException::class)
  fun testConstructorWithNegativeRadius() {
    IterativeBoxBlurPostProcessor(3, -1)
  }

  @Test
  fun testGetPostprocessorCacheKey() {
    val postProcessor = IterativeBoxBlurPostProcessor(3, 10)
    val cacheKey = postProcessor.postprocessorCacheKey
    assertThat(cacheKey).isNotNull()
    assertThat(cacheKey.toString()).contains("i3r10")
  }

  @Test
  fun testCacheKeyIsCached() {
    val postProcessor = IterativeBoxBlurPostProcessor(3, 10)
    val cacheKey1 = postProcessor.postprocessorCacheKey
    val cacheKey2 = postProcessor.postprocessorCacheKey
    assertThat(cacheKey1).isSameAs(cacheKey2)
  }

  @Test
  fun testDifferentParamsProduceDifferentCacheKeys() {
    val postProcessor1 = IterativeBoxBlurPostProcessor(3, 10)
    val postProcessor2 = IterativeBoxBlurPostProcessor(5, 20)
    val cacheKey1 = postProcessor1.postprocessorCacheKey
    val cacheKey2 = postProcessor2.postprocessorCacheKey
    assertThat(cacheKey1).isNotEqualTo(cacheKey2)
  }
}

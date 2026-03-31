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
class RoundedCornersPostprocessorTest {

  @Test
  fun testConstructor() {
    val postProcessor = RoundedCornersPostprocessor()
    assertThat(postProcessor).isNotNull()
  }

  @Test
  fun testGetPostprocessorCacheKey() {
    val postProcessor = RoundedCornersPostprocessor()
    val cacheKey = postProcessor.postprocessorCacheKey
    assertThat(cacheKey).isNotNull()
    assertThat(cacheKey.toString()).contains("RoundedCornersPostprocessor")
  }

  @Test
  fun testCacheKeyIsCached() {
    val postProcessor = RoundedCornersPostprocessor()
    val cacheKey1 = postProcessor.postprocessorCacheKey
    val cacheKey2 = postProcessor.postprocessorCacheKey
    assertThat(cacheKey1).isSameAs(cacheKey2)
  }
}

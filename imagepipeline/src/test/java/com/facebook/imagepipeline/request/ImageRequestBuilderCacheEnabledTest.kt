/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.request

import android.net.Uri
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner

@RunWith(ParameterizedRobolectricTestRunner::class)
class ImageRequestBuilderCacheEnabledTest(
    private val uriScheme: String?,
    private val expectedDefaultDiskCacheEnabled: Boolean,
) {

  @Test
  fun testIsDiskCacheEnabledByDefault() {
    val imageRequestBuilder = createBuilder()
    assertThat(imageRequestBuilder.isDiskCacheEnabled()).isEqualTo(expectedDefaultDiskCacheEnabled)
  }

  @Test
  fun testIsDiskCacheDisabledIfRequested() {
    val imageRequestBuilder = createBuilder()
    imageRequestBuilder.disableDiskCache()
    assertThat(imageRequestBuilder.isDiskCacheEnabled()).isFalse()
  }

  private fun createBuilder(): ImageRequestBuilder {
    return ImageRequestBuilder.newBuilderWithSource(Uri.parse("$uriScheme://request"))
  }

  companion object {
    @ParameterizedRobolectricTestRunner.Parameters(name = "URI of scheme \"{0}://\"")
    @JvmStatic
    fun data(): Collection<Array<Any?>> {
      return listOf(
          arrayOf("asset", false),
          arrayOf("content", false),
          arrayOf("data", false),
          arrayOf("file", false),
          arrayOf("http", true),
          arrayOf("https", true),
          arrayOf("res", false),
      )
    }
  }
}

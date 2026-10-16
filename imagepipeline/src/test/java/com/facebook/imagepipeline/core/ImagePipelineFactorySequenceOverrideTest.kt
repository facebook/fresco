/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.core

import com.facebook.imagepipeline.decoder.ImageDecoder
import com.facebook.imagepipeline.producers.NetworkFetcher
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
class ImagePipelineFactorySequenceOverrideTest {
  @Test
  fun replacementBypassesDefaultProducerDependencies() {
    val replacement = mock(ProducerSequenceFactoryInterface::class.java)
    val baseConfig =
        ImagePipelineConfig.newBuilder(RuntimeEnvironment.getApplication())
            .setProducerSequenceFactory(replacement)
            .build()
    val config =
        object : ImagePipelineConfigInterface by baseConfig {
          override val imageDecoder: ImageDecoder?
            get() = error("Default decoder must not be initialized")

          override val networkFetcher: NetworkFetcher<*>
            get() = error("Default network fetcher must not be used")
        }
    val factory = ImagePipelineFactory(config)
    val pipeline = checkNotNull(
        ReflectionHelpers.callInstanceMethod<ImagePipeline>(factory, "createImagePipeline"),
    )

    assertThat(pipeline.producerSequenceFactory).isSameAs(replacement)
  }

  @Test
  fun nullReplacementKeepsDefaultFactory() {
    val config =
        ImagePipelineConfig.newBuilder(RuntimeEnvironment.getApplication())
            .setProducerSequenceFactory(mock(ProducerSequenceFactoryInterface::class.java))
            .setProducerSequenceFactory(null)
            .setMemoryChunkType(MemoryChunkType.BUFFER_MEMORY)
            .apply { experiment().setNativeCodeDisabled(true) }
            .build()
    val factory = ImagePipelineFactory(config)
    val pipeline = checkNotNull(
        ReflectionHelpers.callInstanceMethod<ImagePipeline>(factory, "createImagePipeline"),
    )

    assertThat(pipeline.producerSequenceFactory).isInstanceOf(ProducerSequenceFactory::class.java)
  }
}

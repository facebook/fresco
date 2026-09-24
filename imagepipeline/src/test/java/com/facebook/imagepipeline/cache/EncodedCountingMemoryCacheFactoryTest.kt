/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache

import com.facebook.cache.common.SimpleCacheKey
import com.facebook.common.internal.Supplier
import com.facebook.common.memory.MemoryTrimmableRegistry
import com.facebook.common.references.CloseableReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class EncodedCountingMemoryCacheFactoryTest {

  @Test
  fun createsCountingCacheForCustomEncodedValue() {
    val memoryTrimmableRegistry = mock<MemoryTrimmableRegistry>()
    val cache =
        EncodedCountingMemoryCacheFactory.get(
            Supplier { MemoryCacheParams(100, 10, 100, 10, 100) },
            memoryTrimmableRegistry,
            MemoryCache.CacheTrimStrategy { 1.0 },
            ValueDescriptor<ByteArray> { it.size },
        )
    val key = SimpleCacheKey("key")
    val value = CloseableReference.of(byteArrayOf(1, 2, 3, 4)) {}

    val cached = cache.cache(key, value)
    value.close()
    val result = cache[key]

    assertThat(result?.get()).containsExactly(1, 2, 3, 4)
    assertThat(cache.sizeInBytes).isEqualTo(4)
    verify(memoryTrimmableRegistry).registerMemoryTrimmable(cache)

    result?.close()
    cached?.close()
  }
}

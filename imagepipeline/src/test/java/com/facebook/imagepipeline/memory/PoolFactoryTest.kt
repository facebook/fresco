/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory

import com.facebook.common.memory.MemoryTrimmable
import com.facebook.common.memory.MemoryTrimmableRegistry
import com.facebook.imagepipeline.core.MemoryChunkType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Tests for [PoolFactory]. */
@RunWith(RobolectricTestRunner::class)
class PoolFactoryTest {

  private val poolFactory = PoolFactory(PoolConfig.newBuilder().build())

  @Test
  fun `native selector returns native pool singleton`() {
    val first = poolFactory.getMemoryChunkPool(MemoryChunkType.NATIVE_MEMORY)

    assertAvailableWhenImplementationIsPresent(
        first,
        "com.facebook.imagepipeline.memory.NativeMemoryChunkPool",
    )
    assertThat(poolFactory.getMemoryChunkPool(MemoryChunkType.NATIVE_MEMORY)).isSameAs(first)
    assertThat(first).isSameAs(poolFactory.nativeMemoryChunkPool)
  }

  @Test
  fun `buffer selector returns buffer pool singleton`() {
    val first = poolFactory.getMemoryChunkPool(MemoryChunkType.BUFFER_MEMORY)

    assertAvailableWhenImplementationIsPresent(
        first,
        "com.facebook.imagepipeline.memory.BufferMemoryChunkPool",
    )
    assertThat(poolFactory.getMemoryChunkPool(MemoryChunkType.BUFFER_MEMORY)).isSameAs(first)
    assertThat(first).isSameAs(poolFactory.bufferMemoryChunkPool)
  }

  @Test
  fun `ashmem selector returns same optional pool repeatedly`() {
    val first = poolFactory.getMemoryChunkPool(MemoryChunkType.ASHMEM_MEMORY)

    assertAvailableWhenImplementationIsPresent(
        first,
        "com.facebook.imagepipeline.memory.AshmemMemoryChunkPool",
    )
    assertThat(poolFactory.getMemoryChunkPool(MemoryChunkType.ASHMEM_MEMORY)).isSameAs(first)
  }

  @Test
  fun `factory does not create a chunk pool before selection`() {
    val registry = TrackingMemoryTrimmableRegistry()

    PoolFactory(PoolConfig.newBuilder().setMemoryTrimmableRegistry(registry).build())

    assertThat(registry.registered).isEmpty()
  }

  @Test
  fun `invalid selector throws`() {
    assertThatThrownBy { poolFactory.getMemoryChunkPool(Int.MIN_VALUE) }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Invalid MemoryChunkType")
  }

  private class TrackingMemoryTrimmableRegistry : MemoryTrimmableRegistry {
    val registered = mutableListOf<MemoryTrimmable>()

    override fun registerMemoryTrimmable(trimmable: MemoryTrimmable) {
      registered.add(trimmable)
    }

    override fun unregisterMemoryTrimmable(trimmable: MemoryTrimmable) {
      registered.remove(trimmable)
    }
  }

  private fun assertAvailableWhenImplementationIsPresent(
      pool: MemoryChunkPool?,
      className: String,
  ) {
    val implementationIsPresent =
        try {
          Class.forName(className)
          true
        } catch (_: ClassNotFoundException) {
          false
        }

    if (implementationIsPresent) {
      assertThat(pool).isNotNull()
    } else {
      assertThat(pool).isNull()
    }
  }
}

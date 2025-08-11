/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.cache.disk

import com.facebook.cache.common.CacheEventListener.EvictionReason
import com.facebook.cache.common.CacheKey
import java.io.IOException
import org.assertj.core.api.Assertions
import org.junit.Test
import org.mockito.Mockito

class SettableCacheEventTest {
  @Test
  fun testRecycleClearsAllFields() {
    val event = SettableCacheEvent.obtain()
    event.setCacheKey(Mockito.mock<CacheKey?>(CacheKey::class.java))
    event.setCacheLimit(21L)
    event.setCacheSize(12332445L)
    event.setEvictionReason(EvictionReason.CACHE_MANAGER_TRIMMED)
    event.setException(Mockito.mock<IOException?>(IOException::class.java))
    event.setItemSize(1435L)
    event.setResourceId("sddqrtyjf")

    event.recycle()

    Assertions.assertThat<CacheKey?>(event.cacheKey).isNull()
    Assertions.assertThat(event.cacheLimit).isZero()
    Assertions.assertThat(event.cacheSize).isZero()
    Assertions.assertThat<EvictionReason?>(event.evictionReason).isNull()
    Assertions.assertThat(event.exception).isNull()
    Assertions.assertThat(event.itemSize).isZero()
    Assertions.assertThat(event.resourceId).isNull()
  }

  @Test
  fun testSecondObtainGetsNewEventIfNoRecycling() {
    val firstEvent = SettableCacheEvent.obtain()
    val secondEvent = SettableCacheEvent.obtain()

    Assertions.assertThat<SettableCacheEvent?>(secondEvent).isNotSameAs(firstEvent)
  }

  @Test
  fun testSecondObtainAfterRecyclingGetsRecycledEvent() {
    val firstEvent = SettableCacheEvent.obtain()
    firstEvent.recycle()
    val secondEvent = SettableCacheEvent.obtain()

    Assertions.assertThat<SettableCacheEvent?>(secondEvent).isSameAs(firstEvent)
  }
}

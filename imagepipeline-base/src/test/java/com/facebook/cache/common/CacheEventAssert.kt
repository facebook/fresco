/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.cache.common

import java.io.IOException
import org.assertj.core.api.AbstractAssert
import org.assertj.core.api.Assertions

/**
 * Assertion methods for [CacheEvent]s.
 *
 * To create an instance of this class, invoke [CacheEventAssert.assertThat].
 */
class CacheEventAssert(actual: CacheEvent) :
    AbstractAssert<CacheEventAssert, CacheEvent>(actual, CacheEventAssert::class.java) {

  fun hasCacheKey(expected: CacheKey?): CacheEventAssert {
    Assertions.assertThat(actual.cacheKey)
        .overridingErrorMessage(
            "Cache event mismatch - cache key <%s> does not match <%s>",
            actual.cacheKey,
            expected,
        )
        .isEqualTo(expected)
    return this
  }

  fun hasException(expected: IOException?): CacheEventAssert {
    Assertions.assertThat(actual.exception)
        .overridingErrorMessage(
            "Cache event mismatch - exception <%s> does not match <%s>",
            actual.cacheKey,
            expected,
        )
        .isEqualTo(expected)
    return this
  }

  fun hasEvictionReason(expected: CacheEventListener.EvictionReason?): CacheEventAssert {
    Assertions.assertThat(actual.evictionReason)
        .overridingErrorMessage(
            "Cache event mismatch - exception <%s> does not match <%s>",
            actual.evictionReason,
            expected,
        )
        .isEqualTo(expected)
    return this
  }

  fun hasItemSize(expected: Long): CacheEventAssert {
    Assertions.assertThat(actual.itemSize)
        .overridingErrorMessage(
            "Cache event mismatch - item size <%s> does not match <%s>",
            actual.itemSize,
            expected,
        )
        .isEqualTo(expected)
    return this
  }

  fun hasCacheSize(expected: Long): CacheEventAssert {
    Assertions.assertThat(actual.cacheSize)
        .overridingErrorMessage(
            "Cache event mismatch - cache size <%s> does not match <%s>",
            actual.cacheSize,
            expected,
        )
        .isEqualTo(expected)
    return this
  }

  fun hasResourceId(expected: String?): CacheEventAssert {
    Assertions.assertThat(actual.resourceId)
        .overridingErrorMessage(
            "Cache event mismatch - resource ID:%s does not match:%s",
            actual.resourceId,
            expected,
        )
        .isEqualTo(expected)
    return this
  }

  fun hasResourceIdSet(): CacheEventAssert {
    Assertions.assertThat(actual.resourceId)
        .overridingErrorMessage("Cache event mismatch - resource ID should not be null")
        .isNotNull()
    return this
  }

  companion object {
    @JvmStatic
    fun assertThat(actual: CacheEvent): CacheEventAssert {
      return CacheEventAssert(actual)
    }
  }
}

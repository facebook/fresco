/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache

import com.facebook.cache.common.CacheKey
import com.facebook.cache.common.SimpleCacheKey
import com.facebook.common.internal.Predicate
import com.facebook.common.references.CloseableReference
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class UnifiedEncodedMemoryCacheTest {

  private val backingCache = mock<MemoryCache<CacheKey, ByteArray>>()

  private val subject = UnifiedEncodedMemoryCache(backingCache)

  @Test
  fun delegatesCacheOperations() {
    val key = SimpleCacheKey("key")
    val reference = CloseableReference.of(byteArrayOf(1, 2, 3)) {}
    try {
      val predicate = Predicate<CacheKey> { it == key }
      whenever(backingCache[key]).thenReturn(reference)
      whenever(backingCache.cache(key, reference)).thenReturn(reference)

      assertSame(reference, subject[key])
      assertSame(reference, subject.put(key, reference))
      subject.remove(key)

      val exactPredicateCaptor = argumentCaptor<Predicate<CacheKey>>()
      verify(backingCache).removeAll(exactPredicateCaptor.capture())
      val exactPredicate = exactPredicateCaptor.firstValue
      assertTrue(exactPredicate.apply(key))
      assertFalse(exactPredicate.apply(SimpleCacheKey("other")))
      subject.removeAll(predicate)
      verify(backingCache).removeAll(predicate)
    } finally {
      reference.close()
    }
  }

  @Test
  fun removeUsesExactKeyCapabilityWhenAvailable() {
    val backingCache = mock<LruCountingMemoryCache<CacheKey, ByteArray>>()
    val subject = UnifiedEncodedMemoryCache(backingCache)
    val key = SimpleCacheKey("key")

    subject.remove(key)

    verify(backingCache).remove(key)
  }
}

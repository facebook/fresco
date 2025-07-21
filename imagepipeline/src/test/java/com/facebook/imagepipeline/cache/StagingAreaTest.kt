/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache

import com.facebook.cache.common.CacheKey
import com.facebook.cache.common.SimpleCacheKey
import com.facebook.common.memory.PooledByteBuffer
import com.facebook.common.references.CloseableReference
import com.facebook.imagepipeline.image.EncodedImage
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class StagingAreaTest {
  private lateinit var stagingArea: StagingArea
  private lateinit var closeableReference: CloseableReference<PooledByteBuffer>
  private lateinit var closeableReference2: CloseableReference<PooledByteBuffer>
  private lateinit var encodedImage: EncodedImage
  private lateinit var secondEncodedImage: EncodedImage
  private lateinit var cacheKey: CacheKey

  @Before
  fun setUp() {
    stagingArea = StagingArea.getInstance()
    closeableReference = CloseableReference.of(mock())
    closeableReference2 = CloseableReference.of(mock())
    encodedImage = EncodedImage(closeableReference)
    secondEncodedImage = EncodedImage(closeableReference2)
    cacheKey = SimpleCacheKey("http://this.is/uri")
    stagingArea.put(cacheKey, encodedImage)
  }

  @Test
  fun testContains() {
    assertThat(stagingArea.containsKey(cacheKey)).isTrue()
    assertThat(stagingArea.containsKey(SimpleCacheKey("http://this.is.not.uri"))).isFalse()
  }

  @Test
  fun testDoesntContainInvalid() {
    encodedImage.close()
    assertThat(stagingArea.containsKey(cacheKey)).isTrue()
    assertThat(EncodedImage.isValid(stagingArea.get(cacheKey))).isTrue()
  }

  @Test
  fun testGetValue() {
    val image = stagingArea.get(cacheKey)
    assertThat(image).isNotNull()
    assertThat(image?.byteBufferRef?.underlyingReferenceTestOnly)
        .isSameAs(closeableReference.underlyingReferenceTestOnly)
  }

  @Test
  fun testBumpsRefCountOnGet() {
    stagingArea.get(cacheKey)
    assertThat(closeableReference.underlyingReferenceTestOnly.refCountTestOnly).isEqualTo(4)
  }

  @Test
  fun testAnotherPut() {
    stagingArea.put(cacheKey, secondEncodedImage)
    assertThat(closeableReference.underlyingReferenceTestOnly.refCountTestOnly).isEqualTo(2)
    val image = stagingArea.get(cacheKey)
    assertThat(image).isNotNull()
    assertThat(image?.byteBufferRef?.underlyingReferenceTestOnly)
        .isSameAs(closeableReference2.underlyingReferenceTestOnly)
  }

  @Test
  fun testSamePut() {
    assertThat(closeableReference.underlyingReferenceTestOnly.refCountTestOnly).isEqualTo(3)
    stagingArea.put(cacheKey, encodedImage)
    assertThat(closeableReference.underlyingReferenceTestOnly.refCountTestOnly).isEqualTo(3)
    val image = stagingArea.get(cacheKey)
    assertThat(image).isNotNull()
    assertThat(image?.byteBufferRef?.underlyingReferenceTestOnly)
        .isSameAs(closeableReference.underlyingReferenceTestOnly)
  }

  @Test
  fun testRemove() {
    assertThat(stagingArea.remove(cacheKey, encodedImage)).isTrue()
    assertThat(closeableReference.underlyingReferenceTestOnly.refCountTestOnly).isEqualTo(2)
    assertThat(stagingArea.remove(cacheKey, encodedImage)).isFalse()
  }

  @Test
  fun testRemoveWithBadRef() {
    assertThat(stagingArea.remove(cacheKey, secondEncodedImage)).isFalse()
    assertThat(CloseableReference.isValid(closeableReference)).isTrue()
    assertThat(CloseableReference.isValid(closeableReference2)).isTrue()
  }

  @Test
  fun testRemoveWithoutValueCheck() {
    assertThat(stagingArea.remove(cacheKey)).isTrue()
    assertThat(closeableReference.underlyingReferenceTestOnly.refCountTestOnly).isEqualTo(2)
    assertThat(stagingArea.remove(cacheKey)).isFalse()
  }

  @Test
  fun testClearAll() {
    stagingArea.put(SimpleCacheKey("second"), secondEncodedImage)
    stagingArea.clearAll()
    assertThat(closeableReference.underlyingReferenceTestOnly.refCountTestOnly).isEqualTo(2)
    assertThat(closeableReference2.underlyingReferenceTestOnly.refCountTestOnly).isEqualTo(2)
    assertThat(stagingArea.remove(cacheKey)).isFalse()
  }
}

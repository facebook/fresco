/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import com.facebook.common.references.CloseableReference
import com.facebook.datasource.SimpleDataSource
import com.facebook.imagepipeline.image.CloseableImage
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Unit tests for [KFrescoVitoDrawable.setDataSource]. */
@RunWith(RobolectricTestRunner::class)
class KFrescoVitoDrawableDataSourceTest {

  private lateinit var drawable: KFrescoVitoDrawable

  @Before
  fun setUp() {
    drawable = KFrescoVitoDrawable()
    drawable._imageId = 1L
  }

  @Test
  fun testSetDataSource_matchingImageId_storesAndClosesPrevious() {
    val first = SimpleDataSource.create<CloseableReference<CloseableImage>>()
    val second = SimpleDataSource.create<CloseableReference<CloseableImage>>()

    drawable.setDataSource(1L, first)
    assertThat(drawable.dataSource).isSameAs(first)

    drawable.setDataSource(1L, second)
    assertThat(drawable.dataSource).isSameAs(second)
    assertThat(first.isClosed).isTrue()
    assertThat(second.isClosed).isFalse()
  }

  @Test
  fun testSetDataSource_staleImageId_ignored() {
    val current = SimpleDataSource.create<CloseableReference<CloseableImage>>()
    val stale = SimpleDataSource.create<CloseableReference<CloseableImage>>()
    drawable.setDataSource(1L, current)

    drawable.setDataSource(2L, stale)

    assertThat(drawable.dataSource).isSameAs(current)
    assertThat(stale.isClosed).isFalse()
  }

  @Test
  fun testSetDataSource_matchingImageIdNull_clearsAndClosesPrevious() {
    val current = SimpleDataSource.create<CloseableReference<CloseableImage>>()
    drawable.setDataSource(1L, current)

    drawable.setDataSource(1L, null)

    assertThat(drawable.dataSource).isNull()
    assertThat(current.isClosed).isTrue()
  }
}

/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root
 * directory of this source tree.
 */

package com.facebook.imagepipeline.datasource

import com.facebook.common.executors.CallerThreadExecutor
import com.facebook.common.references.CloseableReference
import com.facebook.common.references.ResourceReleaser
import com.facebook.datasource.DataSource
import com.facebook.datasource.DataSubscriber
import java.util.concurrent.CancellationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ListDataSourceTest {
  private var settableDataSource1: SettableDataSource<Int?>? = null
  private var settableDataSource2: SettableDataSource<Int?>? = null
  private var listDataSource: ListDataSource<Int?>? = null
  private var ref1: CloseableReference<Int?>? = null
  private var ref2: CloseableReference<Int?>? = null
  private var runtimeException: RuntimeException? = null

  private val resourceReleaser: ResourceReleaser<Int?> = mock()
  private val dataSubscriber: DataSubscriber<MutableList<CloseableReference<Int?>?>?> = mock()

  @Before
  fun setUp() {
    settableDataSource1 = SettableDataSource.create<Int?>()
    settableDataSource2 = SettableDataSource.create<Int?>()
    listDataSource = ListDataSource.create<Int?>(settableDataSource1, settableDataSource2)
    ref1 = CloseableReference.of<Int?>(1, resourceReleaser)
    ref2 = CloseableReference.of<Int?>(2, resourceReleaser)
    runtimeException = RuntimeException()
    listDataSource?.subscribe(dataSubscriber, CallerThreadExecutor.getInstance())
  }

  @Test
  fun testFirstResolvedSecondNot() {
    resolveFirstDataSource()
    assertDataSourceNotResolved()
  }

  @Test
  fun testSecondResolvedFirstNot() {
    resolveSecondDataSource()
    assertDataSourceNotResolved()
  }

  @Test
  fun testFirstCancelledSecondNot() {
    cancelFirstDataSource()
    assertDataSourceCancelled()
  }

  @Test
  fun testSecondCancelledFirstNot() {
    cancelSecondDataSource()
    assertDataSourceCancelled()
  }

  @Test
  fun testFirstFailedSecondNot() {
    failFirstDataSource()
    assertDataSourceFailed()
  }

  @Test
  fun testSecondFailedFirstNot() {
    failSecondDataSource()
    assertDataSourceFailed()
  }

  @Test
  fun testFirstResolvedSecondFailed() {
    resolveFirstDataSource()
    failSecondDataSource()
    assertDataSourceFailed()
  }

  @Test
  fun testSecondResolvedFirstFailed() {
    failFirstDataSource()
    resolveSecondDataSource()
    assertDataSourceFailed()
  }

  @Test
  fun testFirstResolvedSecondCancelled() {
    resolveFirstDataSource()
    cancelSecondDataSource()
    assertDataSourceCancelled()
  }

  @Test
  fun testSecondResolvedFirstCancelled() {
    resolveSecondDataSource()
    cancelFirstDataSource()
    assertDataSourceCancelled()
  }

  @Test
  fun testFirstAndSecondResolved() {
    resolveFirstDataSource()
    resolveSecondDataSource()
    assertDataSourceResolved()
  }

  @Test
  fun testCloseClosesAllDataSources() {
    listDataSource?.close()
    assertThat(settableDataSource1?.isClosed()).isTrue()
    assertThat(settableDataSource2?.isClosed()).isTrue()
  }

  private fun failFirstDataSource() {
    runtimeException?.let { settableDataSource1?.setException(it) }
  }

  private fun failSecondDataSource() {
    runtimeException?.let { settableDataSource2?.setException(it) }
  }

  private fun cancelFirstDataSource() {
    settableDataSource1?.close()
  }

  private fun cancelSecondDataSource() {
    settableDataSource2?.close()
  }

  private fun resolveFirstDataSource() {
    settableDataSource1?.set(ref1)
  }

  private fun resolveSecondDataSource() {
    settableDataSource2?.set(ref2)
  }

  private fun assertDataSourceNotResolved() {
    verifyNoMoreInteractions(dataSubscriber)
    assertThat(listDataSource?.hasResult()).isFalse()
    assertThat(listDataSource?.hasFailed()).isFalse()
    assertThat(listDataSource?.isFinished()).isFalse()
    assertThat(listDataSource?.getFailureCause()).isNull()
    assertThat(listDataSource?.getResult()).isNull()
  }

  private fun assertDataSourceFailed() {
    val dataSource = listDataSource as? DataSource<MutableList<CloseableReference<Int?>?>?>
    requireNotNull(dataSource) { "listDataSource should not be null" }

    verify(dataSubscriber).onFailure(dataSource)
    verifyNoMoreInteractions(dataSubscriber)
    assertThat(listDataSource?.hasResult()).isFalse()
    assertThat(listDataSource?.hasFailed()).isTrue()
    assertThat(listDataSource?.isFinished()).isTrue()
    assertThat(listDataSource?.getFailureCause()).isSameAs(runtimeException)
    assertThat(listDataSource?.getResult()).isNull()
  }

  private fun assertDataSourceCancelled() {
    val dataSource = listDataSource as? DataSource<MutableList<CloseableReference<Int?>?>?>
    requireNotNull(dataSource) { "listDataSource should not be null" }

    verify(dataSubscriber).onFailure(dataSource)
    verifyNoMoreInteractions(dataSubscriber)
    assertThat(listDataSource?.hasResult()).isFalse()
    assertThat(listDataSource?.hasFailed()).isTrue()
    assertThat(listDataSource?.isFinished()).isTrue()
    assertThat(listDataSource?.getFailureCause()).isInstanceOf(CancellationException::class.java)
    assertThat(listDataSource?.getResult()).isNull()
  }

  private fun assertDataSourceResolved() {
    val dataSource = listDataSource as? DataSource<MutableList<CloseableReference<Int?>?>?>
    requireNotNull(dataSource) { "listDataSource should not be null" }

    verify(dataSubscriber).onNewResult(dataSource)
    verifyNoMoreInteractions(dataSubscriber)
    assertThat(listDataSource?.hasResult()).isTrue()
    assertThat(listDataSource?.hasFailed()).isFalse()
    assertThat(listDataSource?.isFinished()).isTrue()
    assertThat(listDataSource?.getFailureCause()).isNull()

    val result = listDataSource?.getResult()
    assertThat(result).isNotNull
    assertThat(result?.size).isEqualTo(2)
    assertThat(result?.get(0)?.get()).isEqualTo(1)
    assertThat(result?.get(1)?.get()).isEqualTo(2)
  }
}

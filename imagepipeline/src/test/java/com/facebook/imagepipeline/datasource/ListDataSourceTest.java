/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.datasource;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.facebook.common.executors.CallerThreadExecutor;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.references.ResourceReleaser;
import com.facebook.datasource.DataSubscriber;
import java.util.List;
import java.util.concurrent.CancellationException;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.robolectric.*;

@RunWith(RobolectricTestRunner.class)
public class ListDataSourceTest {

  private SettableDataSource<Integer> mSettableDataSource1;
  private SettableDataSource<Integer> mSettableDataSource2;
  private ListDataSource<Integer> mListDataSource;
  private CloseableReference<Integer> mRef1;
  private CloseableReference<Integer> mRef2;
  private RuntimeException mRuntimeException;

  @Mock public ResourceReleaser<Integer> mResourceReleaser;
  @Mock public DataSubscriber<List<CloseableReference<Integer>>> mDataSubscriber;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    mSettableDataSource1 = SettableDataSource.create();
    mSettableDataSource2 = SettableDataSource.create();
    mListDataSource = ListDataSource.create(mSettableDataSource1, mSettableDataSource2);
    mRef1 = CloseableReference.of(1, mResourceReleaser);
    mRef2 = CloseableReference.of(2, mResourceReleaser);
    mRuntimeException = new RuntimeException();
    mListDataSource.subscribe(mDataSubscriber, CallerThreadExecutor.getInstance());
  }

  @Test
  public void testFirstResolvedSecondNot() {
    resolveFirstDataSource();
    assertDataSourceNotResolved();
  }

  @Test
  public void testSecondResolvedFirstNot() {
    resolveSecondDataSource();
    assertDataSourceNotResolved();
  }

  @Test
  public void testFirstCancelledSecondNot() {
    cancelFirstDataSource();
    assertDataSourceCancelled();
  }

  @Test
  public void testSecondCancelledFirstNot() {
    cancelSecondDataSource();
    assertDataSourceCancelled();
  }

  @Test
  public void testFirstFailedSecondNot() {
    failFirstDataSource();
    assertDataSourceFailed();
  }

  @Test
  public void testSecondFailedFirstNot() {
    failSecondDataSource();
    assertDataSourceFailed();
  }

  @Test
  public void testFirstResolvedSecondFailed() {
    resolveFirstDataSource();
    failSecondDataSource();
    assertDataSourceFailed();
  }

  @Test
  public void testSecondResolvedFirstFailed() {
    failFirstDataSource();
    resolveSecondDataSource();
    assertDataSourceFailed();
  }

  @Test
  public void testFirstResolvedSecondCancelled() {
    resolveFirstDataSource();
    cancelSecondDataSource();
    assertDataSourceCancelled();
  }

  @Test
  public void testSecondResolvedFirstCancelled() {
    resolveSecondDataSource();
    cancelFirstDataSource();
    assertDataSourceCancelled();
  }

  @Test
  public void testFirstAndSecondResolved() {
    resolveFirstDataSource();
    resolveSecondDataSource();
    assertDataSourceResolved();
  }

  @Test
  public void testCloseClosesAllDataSources() {
    mListDataSource.close();
    assertTrue(mSettableDataSource1.isClosed());
    assertTrue(mSettableDataSource2.isClosed());
  }

  private void failFirstDataSource() {
    mSettableDataSource1.setException(mRuntimeException);
  }

  private void failSecondDataSource() {
    mSettableDataSource2.setException(mRuntimeException);
  }

  private void cancelFirstDataSource() {
    mSettableDataSource1.close();
  }

  private void cancelSecondDataSource() {
    mSettableDataSource2.close();
  }

  private void resolveFirstDataSource() {
    mSettableDataSource1.set(mRef1);
  }

  private void resolveSecondDataSource() {
    mSettableDataSource2.set(mRef2);
  }

  private void assertDataSourceNotResolved() {
    verifyNoMoreInteractions(mDataSubscriber);
    assertFalse(mListDataSource.hasResult());
    assertFalse(mListDataSource.hasFailed());
    assertFalse(mListDataSource.isFinished());
    assertNull(mListDataSource.getFailureCause());
    assertNull(mListDataSource.getResult());
  }

  private void assertDataSourceFailed() {
    verify(mDataSubscriber).onFailure(mListDataSource);
    verifyNoMoreInteractions(mDataSubscriber);
    assertFalse(mListDataSource.hasResult());
    assertTrue(mListDataSource.hasFailed());
    assertTrue(mListDataSource.isFinished());
    assertSame(mRuntimeException, mListDataSource.getFailureCause());
    assertNull(mListDataSource.getResult());
  }

  private void assertDataSourceCancelled() {
    verify(mDataSubscriber).onFailure(mListDataSource);
    verifyNoMoreInteractions(mDataSubscriber);
    assertFalse(mListDataSource.hasResult());
    assertTrue(mListDataSource.hasFailed());
    assertTrue(mListDataSource.isFinished());
    assertTrue(mListDataSource.getFailureCause() instanceof CancellationException);
    assertNull(mListDataSource.getResult());
  }

  private void assertDataSourceResolved() {
    verify(mDataSubscriber).onNewResult(mListDataSource);
    verifyNoMoreInteractions(mDataSubscriber);
    assertTrue(mListDataSource.hasResult());
    assertFalse(mListDataSource.hasFailed());
    assertTrue(mListDataSource.isFinished());
    assertNull(mListDataSource.getFailureCause());
    assertEquals(2, mListDataSource.getResult().size());
    assertEquals(1, (int) mListDataSource.getResult().get(0).get());
    assertEquals(2, (int) mListDataSource.getResult().get(1).get());
  }
}

/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.datasource;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareOnlyThisForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
@PrepareOnlyThisForTest({DataSources.class})
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "androidx.*", "android.*"})
public class DataSourcesTest {

  @Rule
  public PowerMockRule rule = new PowerMockRule();

  private CountDownLatch mCountDownLatch;
  private Exception mException;
  private DataSource<Object> mDataSource;

  private final Object mFinalResult = "final";
  private final Object mIntermediateResult = "intermediate";

  @Before
  public void setUp() throws Exception {
    mException = mock(Exception.class);
    mDataSource = mock(DataSource.class);

    PowerMockito.mockStatic(CountDownLatch.class);
    mCountDownLatch = mock(CountDownLatch.class);
    PowerMockito.whenNew(CountDownLatch.class).withAnyArguments().thenReturn(mCountDownLatch);
  }

  @Test
  public void testImmediateFailedDataSource() {
    DataSource<?> dataSource = DataSources.immediateFailedDataSource(mException);
    assertTrue(dataSource.isFinished());
    assertTrue(dataSource.hasFailed());
    assertEquals(mException, dataSource.getFailureCause());
    assertFalse(dataSource.hasResult());
    assertFalse(dataSource.isClosed());
  }

  @Test
  public void testWaitForFinalResult_whenFinalResult_thenReturnFinalResult() throws Throwable {
    when(mDataSource.isFinished()).thenReturn(true);
    when(mDataSource.getResult()).thenReturn(mFinalResult);

    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        final Object[] args = invocation.getArguments();
        DataSubscriber dataSubscriber = (DataSubscriber) args[0];
        dataSubscriber.onNewResult(mDataSource);
        return null;
      }
    }).when(mDataSource).subscribe(any(DataSubscriber.class), any(Executor.class));

    final Object actual = DataSources.waitForFinalResult(mDataSource);
    assertEquals(mFinalResult, actual);

    verify(mCountDownLatch, times(1)).await();
    verify(mCountDownLatch, times(1)).countDown();
  }

  @Test
  public void testWaitForFinalResult_whenOnlyIntermediateResult_thenNoUpdate() throws Throwable {
    when(mDataSource.isFinished()).thenReturn(false);
    when(mDataSource.getResult()).thenReturn(mIntermediateResult);

    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        final Object[] args = invocation.getArguments();
        DataSubscriber dataSubscriber = (DataSubscriber) args[0];
        dataSubscriber.onNewResult(mDataSource);
        return null;
      }
    }).when(mDataSource).subscribe(any(DataSubscriber.class), any(Executor.class));

    // the mocked one falls through, but the real one waits with the countdown latch for isFinished
    final Object actual = DataSources.waitForFinalResult(mDataSource);
    assertEquals(null, actual);

    verify(mCountDownLatch, times(1)).await();
    verify(mCountDownLatch, never()).countDown();
  }

  @Test
  public void testWaitForFinalResult_whenCancelled_thenReturnNull() throws Throwable {
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        final Object[] args = invocation.getArguments();
        DataSubscriber dataSubscriber = (DataSubscriber) args[0];
        dataSubscriber.onCancellation(mDataSource);
        return null;
      }
    }).when(mDataSource).subscribe(any(DataSubscriber.class), any(Executor.class));

    final Object actual = DataSources.waitForFinalResult(mDataSource);
    assertEquals(null, actual);

    verify(mCountDownLatch, times(1)).await();
    verify(mCountDownLatch, times(1)).countDown();
  }

  @Test
  public void testWaitForFinalResult_whenFailed_thenThrow() throws Throwable {
    final Exception expectedException = new IOException("failure failure");
    when(mDataSource.getFailureCause()).thenReturn(expectedException);

    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        final Object[] args = invocation.getArguments();
        DataSubscriber dataSubscriber = (DataSubscriber) args[0];
        dataSubscriber.onFailure(mDataSource);
        return null;
      }
    }).when(mDataSource).subscribe(any(DataSubscriber.class), any(Executor.class));

    try {
      DataSources.waitForFinalResult(mDataSource);
      fail("expected exception");
    } catch (Exception exception) {
      assertEquals(expectedException, exception);
    }

    verify(mCountDownLatch, times(1)).await();
    verify(mCountDownLatch, times(1)).countDown();
  }
}

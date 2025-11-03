/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mockConstruction;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedConstruction;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class DataSourcesTest {

  private CountDownLatch mCountDownLatch;
  private Exception mException;
  private DataSource<Object> mDataSource;
  private MockedConstruction<CountDownLatch> mMockedCountDownLatchConstruction;

  private final Object mFinalResult = "final";
  private final Object mIntermediateResult = "intermediate";

  @Before
  public void setUp() throws Exception {
    mException = mock(Exception.class);
    mDataSource = mock(DataSource.class);
    mMockedCountDownLatchConstruction =
        mockConstruction(
            CountDownLatch.class,
            (mock, context) -> {
              mCountDownLatch = mock;
            });
  }

  @After
  public void tearDown() {
    if (mMockedCountDownLatchConstruction != null) {
      mMockedCountDownLatchConstruction.close();
    }
  }

  @Test
  public void testImmediateFailedDataSource() {
    DataSource<?> dataSource = DataSources.immediateFailedDataSource(mException);
    assertThat(dataSource.isFinished()).isTrue();
    assertThat(dataSource.hasFailed()).isTrue();
    assertThat(dataSource.getFailureCause()).isEqualTo(mException);
    assertThat(dataSource.hasResult()).isFalse();
    assertThat(dataSource.isClosed()).isFalse();
  }

  @Test
  public void testWaitForFinalResult_whenFinalResult_thenReturnFinalResult() throws Throwable {
    when(mDataSource.isFinished()).thenReturn(true);
    when(mDataSource.getResult()).thenReturn(mFinalResult);

    doAnswer(
            new Answer<Void>() {
              @Override
              public Void answer(InvocationOnMock invocation) throws Throwable {
                final Object[] args = invocation.getArguments();
                DataSubscriber dataSubscriber = (DataSubscriber) args[0];
                dataSubscriber.onNewResult(mDataSource);
                return null;
              }
            })
        .when(mDataSource)
        .subscribe(any(DataSubscriber.class), any(Executor.class));

    final Object actual = DataSources.waitForFinalResult(mDataSource);
    assertThat(actual).isEqualTo(mFinalResult);

    verify(mCountDownLatch, times(1)).await();
    verify(mCountDownLatch, times(1)).countDown();
  }

  @Test
  public void testWaitForFinalResult_withTimeout() throws Throwable {
    when(mDataSource.isFinished()).thenReturn(true);
    when(mDataSource.getResult()).thenReturn(mFinalResult);

    final Object initial = new Object();
    Object actual = initial;
    Throwable throwable = null;
    try {
      actual = DataSources.waitForFinalResult(mDataSource, 1, TimeUnit.MILLISECONDS);
    } catch (Throwable t) {
      throwable = t;
    }
    assertThat(actual).isEqualTo(initial);
    assertThat(throwable instanceof TimeoutException).isTrue();

    verify(mCountDownLatch, times(1)).await(1, TimeUnit.MILLISECONDS);
    verify(mCountDownLatch, times(0)).countDown();
  }

  @Test
  public void testWaitForFinalResult_whenOnlyIntermediateResult_thenNoUpdate() throws Throwable {
    when(mDataSource.isFinished()).thenReturn(false);
    when(mDataSource.getResult()).thenReturn(mIntermediateResult);

    doAnswer(
            new Answer<Void>() {
              @Override
              public Void answer(InvocationOnMock invocation) throws Throwable {
                final Object[] args = invocation.getArguments();
                DataSubscriber dataSubscriber = (DataSubscriber) args[0];
                dataSubscriber.onNewResult(mDataSource);
                return null;
              }
            })
        .when(mDataSource)
        .subscribe(any(DataSubscriber.class), any(Executor.class));

    // the mocked one falls through, but the real one waits with the countdown latch for isFinished
    final Object actual = DataSources.waitForFinalResult(mDataSource);
    assertThat(actual).isNull();

    verify(mCountDownLatch, times(1)).await();
    verify(mCountDownLatch, never()).countDown();
  }

  @Test
  public void testWaitForFinalResult_whenCancelled_thenReturnNull() throws Throwable {
    doAnswer(
            new Answer<Void>() {
              @Override
              public Void answer(InvocationOnMock invocation) throws Throwable {
                final Object[] args = invocation.getArguments();
                DataSubscriber dataSubscriber = (DataSubscriber) args[0];
                dataSubscriber.onCancellation(mDataSource);
                return null;
              }
            })
        .when(mDataSource)
        .subscribe(any(DataSubscriber.class), any(Executor.class));

    final Object actual = DataSources.waitForFinalResult(mDataSource);
    assertThat(actual).isNull();

    verify(mCountDownLatch, times(1)).await();
    verify(mCountDownLatch, times(1)).countDown();
  }

  @Test
  public void testWaitForFinalResult_whenFailed_thenThrow() throws Throwable {
    final Exception expectedException = new IOException("failure failure");
    when(mDataSource.getFailureCause()).thenReturn(expectedException);

    doAnswer(
            new Answer<Void>() {
              @Override
              public Void answer(InvocationOnMock invocation) throws Throwable {
                final Object[] args = invocation.getArguments();
                DataSubscriber dataSubscriber = (DataSubscriber) args[0];
                dataSubscriber.onFailure(mDataSource);
                return null;
              }
            })
        .when(mDataSource)
        .subscribe(any(DataSubscriber.class), any(Executor.class));

    try {
      DataSources.waitForFinalResult(mDataSource);
      fail("expected exception");
    } catch (Exception exception) {
      assertThat(exception).isEqualTo(expectedException);
    }

    verify(mCountDownLatch, times(1)).await();
    verify(mCountDownLatch, times(1)).countDown();
  }
}

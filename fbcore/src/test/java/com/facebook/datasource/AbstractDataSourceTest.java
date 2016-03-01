/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.datasource;

import javax.annotation.Nullable;

import java.util.concurrent.Executor;

import org.robolectric.RobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import static com.facebook.datasource.DataSourceTestUtils.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class AbstractDataSourceTest {

  public interface Value {
    public void close();
  }

  private static class FakeAbstractDataSource extends AbstractDataSource<Value> {
    @Override
    public boolean setResult(@Nullable Value value, boolean isLast) {
      return super.setResult(value, isLast);
    }

    @Override
    public boolean setFailure(Throwable throwable) {
      return super.setFailure(throwable);
    }

    @Override
    public boolean setProgress(float progress) {
      return super.setProgress(progress);
    }

    @Override
    public void closeResult(Value result) {
      result.close();
    }
  }

  private Executor mExecutor1;
  private Executor mExecutor2;
  private DataSubscriber<Value> mDataSubscriber1;
  private DataSubscriber<Value> mDataSubscriber2;
  private FakeAbstractDataSource mDataSource;

  @Before
  public void setUp() {
    mExecutor1 = mock(Executor.class);
    mExecutor2 = mock(Executor.class);
    mDataSubscriber1 = mock(DataSubscriber.class);
    mDataSubscriber2 = mock(DataSubscriber.class);
    mDataSource = new FakeAbstractDataSource();
  }

  private void verifyExecutor(Executor executor) {
    ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
    verify(executor).execute(captor.capture());
    Runnable runnable = captor.getValue();
    assertNotNull(runnable);
    runnable.run();
  }

  private void verifySubscribers(int expected) {
    switch (expected) {
      case NO_INTERACTIONS:
        verifyZeroInteractions(mExecutor1, mDataSubscriber1);
        verifyZeroInteractions(mExecutor2, mDataSubscriber2);
        break;
      case ON_NEW_RESULT:
        verifyExecutor(mExecutor1);
        verify(mDataSubscriber1).onNewResult(mDataSource);
        verifyExecutor(mExecutor2);
        verify(mDataSubscriber2).onNewResult(mDataSource);
        break;
      case ON_FAILURE:
        verifyExecutor(mExecutor1);
        verify(mDataSubscriber1).onFailure(mDataSource);
        verifyExecutor(mExecutor2);
        verify(mDataSubscriber2).onFailure(mDataSource);
        break;
      case ON_CANCELLATION:
        verifyExecutor(mExecutor1);
        verify(mDataSubscriber1).onCancellation(mDataSource);
        verifyExecutor(mExecutor2);
        verify(mDataSubscriber2).onCancellation(mDataSource);
        break;
    }
    reset(mExecutor1, mExecutor2, mDataSubscriber1, mDataSubscriber2);
  }

  private void subscribe() {
    mDataSource.subscribe(mDataSubscriber1, mExecutor1);
    mDataSource.subscribe(mDataSubscriber2, mExecutor2);
  }

  @Test
  public void testInitialState() {
    verifyState(mDataSource, NOT_CLOSED, NOT_FINISHED, WITHOUT_RESULT, null, NOT_FAILED, null);
  }

  @Test
  public void testLifeCycle_LastResult_Close() {
    subscribe();
    // last result
    Value value = mock(Value.class);
    mDataSource.setResult(value, LAST);
    verifySubscribers(ON_NEW_RESULT);
    verifyState(mDataSource, NOT_CLOSED, FINISHED, WITH_RESULT, value, NOT_FAILED, null);
    // close
    mDataSource.close();
    verifySubscribers(NO_INTERACTIONS);
    verifyState(mDataSource, CLOSED, FINISHED, WITHOUT_RESULT, null, NOT_FAILED, null);
  }

  @Test
  public void testLifeCycle_Failure_Close() {
    subscribe();
    // failure
    Throwable throwable = mock(Throwable.class);
    mDataSource.setFailure(throwable);
    verifySubscribers(ON_FAILURE);
    verifyState(mDataSource, NOT_CLOSED, FINISHED, WITHOUT_RESULT, null, FAILED, throwable);
    // close
    mDataSource.close();
    verifySubscribers(NO_INTERACTIONS);
    verifyState(mDataSource, CLOSED, FINISHED, WITHOUT_RESULT, null, FAILED, throwable);
  }

  @Test
  public void testLifeCycle_IntermediateResult_LastResult_Close() {
    subscribe();
    // intermediate result
    Value value1 = mock(Value.class);
    mDataSource.setResult(value1, INTERMEDIATE);
    verifySubscribers(ON_NEW_RESULT);
    verifyState(mDataSource, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, value1, NOT_FAILED, null);
    // last result
    Value value = mock(Value.class);
    mDataSource.setResult(value, LAST);
    verifySubscribers(ON_NEW_RESULT);
    verifyState(mDataSource, NOT_CLOSED, FINISHED, WITH_RESULT, value, NOT_FAILED, null);
    // close
    mDataSource.close();
    verifySubscribers(NO_INTERACTIONS);
    verifyState(mDataSource, CLOSED, FINISHED, WITHOUT_RESULT, null, NOT_FAILED, null);
  }

  @Test
  public void testLifeCycle_IntermediateResult_Failure_Close() {
    subscribe();
    // intermediate result
    Value value1 = mock(Value.class);
    mDataSource.setResult(value1, INTERMEDIATE);
    verifySubscribers(ON_NEW_RESULT);
    verifyState(mDataSource, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, value1, NOT_FAILED, null);
    // failure
    Throwable throwable = mock(Throwable.class);
    mDataSource.setFailure(throwable);
    verifySubscribers(ON_FAILURE);
    verifyState(mDataSource, NOT_CLOSED, FINISHED, WITH_RESULT, value1, FAILED, throwable);
    // close
    mDataSource.close();
    verifySubscribers(NO_INTERACTIONS);
    verifyState(mDataSource, CLOSED, FINISHED, WITHOUT_RESULT, null, FAILED, throwable);
  }

  @Test
  public void testLifeCycle_AfterSuccess() {
    subscribe();
    // success
    Value value = mock(Value.class);
    mDataSource.setResult(value, LAST);
    verifySubscribers(ON_NEW_RESULT);
    verifyState(mDataSource, NOT_CLOSED, FINISHED, WITH_RESULT, value, NOT_FAILED, null);
    // try intermediate
    mDataSource.setResult(mock(Value.class), INTERMEDIATE);
    verifySubscribers(NO_INTERACTIONS);
    verifyState(mDataSource, NOT_CLOSED, FINISHED, WITH_RESULT, value, NOT_FAILED, null);
    // try last
    mDataSource.setResult(mock(Value.class), LAST);
    verifySubscribers(NO_INTERACTIONS);
    verifyState(mDataSource, NOT_CLOSED, FINISHED, WITH_RESULT, value, NOT_FAILED, null);
    // try failure
    mDataSource.setFailure(mock(Throwable.class));
    verifySubscribers(NO_INTERACTIONS);
    verifyState(mDataSource, NOT_CLOSED, FINISHED, WITH_RESULT, value, NOT_FAILED, null);
  }

  @Test
  public void testLifeCycle_AfterFailure() {
    subscribe();
    // failure
    Throwable throwable = mock(Throwable.class);
    mDataSource.setFailure(throwable);
    verifySubscribers(ON_FAILURE);
    verifyState(mDataSource, NOT_CLOSED, FINISHED, WITHOUT_RESULT, null, FAILED, throwable);
    // try intermediate
    mDataSource.setResult(mock(Value.class), INTERMEDIATE);
    verifySubscribers(NO_INTERACTIONS);
    verifyState(mDataSource, NOT_CLOSED, FINISHED, WITHOUT_RESULT, null, FAILED, throwable);
    // try last
    mDataSource.setResult(mock(Value.class), LAST);
    verifySubscribers(NO_INTERACTIONS);
    verifyState(mDataSource, NOT_CLOSED, FINISHED, WITHOUT_RESULT, null, FAILED, throwable);
    // try failure
    mDataSource.setFailure(mock(Throwable.class));
    verifySubscribers(NO_INTERACTIONS);
    verifyState(mDataSource, NOT_CLOSED, FINISHED, WITHOUT_RESULT, null, FAILED, throwable);
  }

  @Test
  public void testLifeCycle_AfterClose() {
    subscribe();
    // close
    mDataSource.close();
    verifySubscribers(ON_CANCELLATION);
    verifyState(mDataSource, CLOSED, NOT_FINISHED, WITHOUT_RESULT, null, NOT_FAILED, null);
    // try intermediate
    mDataSource.setResult(mock(Value.class), INTERMEDIATE);
    verifySubscribers(NO_INTERACTIONS);
    verifyState(mDataSource, CLOSED, NOT_FINISHED, WITHOUT_RESULT, null, NOT_FAILED, null);
    // try last
    mDataSource.setResult(mock(Value.class), LAST);
    verifySubscribers(NO_INTERACTIONS);
    verifyState(mDataSource, CLOSED, NOT_FINISHED, WITHOUT_RESULT, null, NOT_FAILED, null);
    // try failure
    mDataSource.setFailure(mock(Throwable.class));
    verifySubscribers(NO_INTERACTIONS);
    verifyState(mDataSource, CLOSED, NOT_FINISHED, WITHOUT_RESULT, null, NOT_FAILED, null);
  }

  @Test
  public void testSubscribe_InProgress_WithoutResult() {
    subscribe();
    verifySubscribers(NO_INTERACTIONS);
  }

  @Test
  public void testSubscribe_InProgress_WithResult() {
    mDataSource.setResult(mock(Value.class), INTERMEDIATE);
    subscribe();
    verifySubscribers(ON_NEW_RESULT);
  }

  @Test
  public void testSubscribe_Finished_WithoutResult() {
    mDataSource.setResult(null, LAST);
    subscribe();
    verifySubscribers(ON_NEW_RESULT);
  }

  @Test
  public void testSubscribe_Finished_WithResult() {
    mDataSource.setResult(mock(Value.class), LAST);
    subscribe();
    verifySubscribers(ON_NEW_RESULT);
  }

  @Test
  public void testSubscribe_Failed_WithoutResult() {
    mDataSource.setFailure(mock(Throwable.class));
    subscribe();
    verifySubscribers(ON_FAILURE);
  }

  @Test
  public void testSubscribe_Failed_WithResult() {
    mDataSource.setResult(mock(Value.class), INTERMEDIATE);
    mDataSource.setFailure(mock(Throwable.class));
    subscribe();
    verifySubscribers(ON_FAILURE);
  }

  @Test
  public void testSubscribe_Closed_AfterSuccess() {
    mDataSource.setResult(mock(Value.class), LAST);
    mDataSource.close();
    subscribe();
    verifySubscribers(NO_INTERACTIONS);
  }

  @Test
  public void testSubscribe_Closed_AfterFailure() {
    mDataSource.setFailure(mock(Throwable.class));
    mDataSource.close();
    subscribe();
    verifySubscribers(NO_INTERACTIONS);
  }

  @Test
  public void testCloseResult() {
    Value value1 = mock(Value.class);
    mDataSource.setResult(value1, false);

    Value value2 = mock(Value.class);
    mDataSource.setResult(value2, false);
    verify(value1).close();
    verify(value2, never()).close();

    Value value3 = mock(Value.class);
    mDataSource.setResult(value3, false);
    verify(value2).close();
    verify(value3, never()).close();

    mDataSource.close();
    verify(value3).close();
  }
}

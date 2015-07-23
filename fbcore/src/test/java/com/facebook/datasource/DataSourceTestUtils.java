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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;

import com.facebook.common.executors.CallerThreadExecutor;
import com.facebook.common.internal.Supplier;

import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.verification.VerificationMode;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DataSourceTestUtils {

  public static final boolean CLOSED = true;
  public static final boolean NOT_CLOSED = false;
  public static final boolean FINISHED = true;
  public static final boolean NOT_FINISHED = false;
  public static final boolean WITH_RESULT = true;
  public static final boolean WITHOUT_RESULT = false;
  public static final boolean FAILED = true;
  public static final boolean NOT_FAILED = false;
  public static final boolean LAST = true;
  public static final boolean INTERMEDIATE = false;
  public static final int NO_INTERACTIONS = 0;
  public static final int ON_NEW_RESULT = 1;
  public static final int ON_FAILURE = 2;
  public static final int ON_CANCELLATION = 3;

  public static VerificationMode optional() {
    return atLeast(0);
  }

  public static void setState(
      DataSource<Object> dataSource,
      boolean isClosed,
      boolean isFinished,
      boolean hasResult,
      Object value,
      boolean hasFailed,
      Throwable failureCause) {
    when(dataSource.isClosed()).thenReturn(isClosed);
    when(dataSource.isFinished()).thenReturn(isFinished);
    when(dataSource.hasResult()).thenReturn(hasResult);
    when(dataSource.getResult()).thenReturn(value);
    when(dataSource.hasFailed()).thenReturn(hasFailed);
    when(dataSource.getFailureCause()).thenReturn(failureCause);
  }

  public static <T> void verifyState(
      DataSource<T> dataSource,
      boolean isClosed,
      boolean isFinished,
      boolean hasResult,
      T result,
      boolean hasFailed,
      Throwable failureCause) {
    assertEquals("isClosed", isClosed, dataSource.isClosed());
    assertEquals("isFinished", isFinished, dataSource.isFinished());
    assertEquals("hasResult", hasResult, dataSource.hasResult());
    assertSame("getResult", result, dataSource.getResult());
    assertEquals("hasFailed", hasFailed, dataSource.hasFailed());
    assertSame("failureCause", failureCause, dataSource.getFailureCause());
  }

  public static class AbstractDataSourceSupplier {

    protected DataSource<Object> mSrc1;
    protected DataSource<Object> mSrc2;
    protected DataSource<Object> mSrc3;
    protected Supplier<DataSource<Object>> mDataSourceSupplier1;
    protected Supplier<DataSource<Object>> mDataSourceSupplier2;
    protected Supplier<DataSource<Object>> mDataSourceSupplier3;
    protected DataSubscriber<Object> mDataSubscriber;
    protected Executor mExecutor;
    protected InOrder mInOrder;
    protected List<Supplier<DataSource<Object>>> mSuppliers;
    protected Supplier<DataSource<Object>> mDataSourceSupplier;

    public void setUp() {
      mSrc1 = mock(DataSource.class);
      mSrc2 = mock(DataSource.class);
      mSrc3 = mock(DataSource.class);
      mDataSourceSupplier1 = mock(Supplier.class);
      mDataSourceSupplier2 = mock(Supplier.class);
      mDataSourceSupplier3 = mock(Supplier.class);
      when(mDataSourceSupplier1.get()).thenReturn(mSrc1);
      when(mDataSourceSupplier2.get()).thenReturn(mSrc2);
      when(mDataSourceSupplier3.get()).thenReturn(mSrc3);
      mDataSubscriber = mock(DataSubscriber.class);
      mExecutor = CallerThreadExecutor.getInstance();
      mInOrder = inOrder(
          mSrc1,
          mSrc2,
          mSrc3,
          mDataSourceSupplier1,
          mDataSourceSupplier2,
          mDataSourceSupplier3,
          mDataSubscriber);
      mSuppliers = Arrays.asList(
          mDataSourceSupplier1,
          mDataSourceSupplier2,
          mDataSourceSupplier3);
    }

    protected void verifyNoMoreInteractionsAll() {
      verifyOptionals(mSrc1);
      verifyOptionals(mSrc2);
      verifyOptionals(mSrc3);
      mInOrder.verifyNoMoreInteractions();
      verifyNoMoreInteractions(
          mSrc1,
          mSrc2,
          mSrc3,
          mDataSourceSupplier1,
          mDataSourceSupplier2,
          mDataSourceSupplier3,
          mDataSubscriber);
    }

    protected void verifyOptionals(DataSource<Object> underlyingDataSource) {
      mInOrder.verify(underlyingDataSource, optional()).isFinished();
      mInOrder.verify(underlyingDataSource, optional()).hasResult();
      mInOrder.verify(underlyingDataSource, optional()).hasFailed();
      verify(underlyingDataSource, optional()).isFinished();
      verify(underlyingDataSource, optional()).hasResult();
      verify(underlyingDataSource, optional()).hasFailed();
    }

    /**
     * Verifies that our mDataSourceSupplier got underlying data source and subscribed to it.
     * Subscriber is returned.
     */
    protected DataSubscriber<Object> verifyGetAndSubscribe(
        Supplier<DataSource<Object>> dataSourceSupplier,
        DataSource<Object> underlyingDataSource,
        boolean expectMoreInteractions) {
      mInOrder.verify(dataSourceSupplier).get();
      ArgumentCaptor<DataSubscriber> captor = ArgumentCaptor.forClass(DataSubscriber.class);
      mInOrder.verify(underlyingDataSource).subscribe(captor.capture(), any(Executor.class));
      if (!expectMoreInteractions) {
        verifyNoMoreInteractionsAll();
      }
      return captor.getValue();
    }

    protected DataSubscriber<Object> verifyGetAndSubscribe(
        Supplier<DataSource<Object>> dataSourceSupplier,
        DataSource<Object> underlyingDataSource) {
      return verifyGetAndSubscribe(dataSourceSupplier, underlyingDataSource, false);
    }

    protected DataSubscriber<Object> verifyGetAndSubscribeM(
        Supplier<DataSource<Object>> dataSourceSupplier,
        DataSource<Object> underlyingDataSource) {
      return verifyGetAndSubscribe(dataSourceSupplier, underlyingDataSource, true);
    }

    /**
     * Verifies that data source provided by our mDataSourceSupplier notified mDataSubscriber.
     */
    protected void verifySubscriber(
        DataSource<Object> dataSource,
        DataSource<Object> underlyingDataSource,
        int expected) {
      switch (expected) {
        case NO_INTERACTIONS:
          verifyNoMoreInteractionsAll();
          break;
        case ON_NEW_RESULT:
          mInOrder.verify(mDataSubscriber).onNewResult(dataSource);
          verifyNoMoreInteractionsAll();
          break;
        case ON_FAILURE:
          mInOrder.verify(underlyingDataSource).getFailureCause();
          mInOrder.verify(mDataSubscriber).onFailure(dataSource);
          verifyNoMoreInteractionsAll();
          break;
        case ON_CANCELLATION:
          verify(mDataSubscriber).onCancellation(dataSource);
          verifyNoMoreInteractionsAll();
          break;
      }
    }

    /**
     * Verifies the state of the data source provided by our mDataSourceSupplier.
     */
    protected void verifyState(
        DataSource<Object> dataSource,
        @Nullable DataSource<Object> dataSourceWithResult,
        boolean isClosed,
        boolean isFinished,
        boolean hasResult,
        Object result,
        boolean hasFailed,
        Throwable failureCause) {
      DataSourceTestUtils.verifyState(
          dataSource, isClosed, isFinished, hasResult, result, hasFailed, failureCause);
      // DataSourceTestUtils.verifyState will call dataSource.getResult() which should forward to
      // underlyingDataSource.getResult()
      if (dataSourceWithResult != null) {
        mInOrder.verify(dataSourceWithResult).getResult();
      }
      verifyNoMoreInteractionsAll();
    }

    /**
     * Verifies that the underlying data sources get closed when data source provided by
     * our mDataSourceSupplier gets closed.
     */
    protected void testClose(
        DataSource<Object> dataSource,
        DataSource<Object>... underlyingDataSources) {
      dataSource.close();
      if (underlyingDataSources != null) {
        for (DataSource<Object> underlyingDataSource : underlyingDataSources) {
          mInOrder.verify(underlyingDataSource, atLeastOnce()).close();
        }
      }
    }

    /**
     * Gets data source from our mDataSourceSupplier and subscribes mDataSubscriber to it.
     * Obtained data source is returned.
     */
    protected DataSource<Object> getAndSubscribe() {
      DataSource<Object> dataSource = mDataSourceSupplier.get();
      dataSource.subscribe(mDataSubscriber, mExecutor);
      return dataSource;
    }

    /** Respond to subscriber with given data source and response. */
    protected static <T> void respond(
        DataSubscriber<T> subscriber,
        DataSource<T> dataSource,
        int response) {
      switch (response) {
        case NO_INTERACTIONS:
          break;
        case ON_NEW_RESULT:
          subscriber.onNewResult(dataSource);
          break;
        case ON_FAILURE:
          subscriber.onFailure(dataSource);
          break;
        case ON_CANCELLATION:
          subscriber.onCancellation(dataSource);
          break;
      }
    }

    /** Schedule response on subscribe. */
    protected static <T> void respondOnSubscribe(
        final DataSource<T> dataSource,
        final int response) {
      doAnswer(
          new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
              DataSubscriber<T> subscriber = (DataSubscriber<T>) invocation.getArguments()[0];
              respond(subscriber, dataSource, response);
              return subscriber;
            }
          }).when(dataSource).subscribe(any(DataSubscriber.class), any(Executor.class));
    }
  }
}

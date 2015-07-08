/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.datasource;

import org.robolectric.RobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.facebook.datasource.DataSourceTestUtils.*;
import static org.mockito.Mockito.*;

/**
 * Tests for FirstAvailableDataSourceSupplier
 */
@RunWith(RobolectricTestRunner.class)
public class FirstAvailableDataSourceSupplierTest extends AbstractDataSourceSupplier {

  @Before
  public void setUp() {
    super.setUp();
    mDataSourceSupplier = FirstAvailableDataSourceSupplier.create(mSuppliers);
  }

  /**
   * All data sources failed, no intermediate results.
   */
  @Test
  public void testLifecycle_F1_F2_F3_C() {
    DataSource<Object> dataSource = getAndSubscribe();
    DataSubscriber<Object> subscriber1 = verifyGetAndSubscribe(mDataSourceSupplier1, mSrc1);

    setState(mSrc1, NOT_CLOSED, FINISHED, WITHOUT_RESULT, null, FAILED, mock(Throwable.class));
    subscriber1.onFailure(mSrc1);
    mInOrder.verify(mSrc1).close();
    DataSubscriber<Object> subscriber2 = verifyGetAndSubscribe(mDataSourceSupplier2, mSrc2);
    verifyState(dataSource, null, NOT_CLOSED, NOT_FINISHED, WITHOUT_RESULT, null, NOT_FAILED, null);

    setState(mSrc2, NOT_CLOSED, FINISHED, WITHOUT_RESULT, null, FAILED, mock(Throwable.class));
    subscriber2.onFailure(mSrc2);
    mInOrder.verify(mSrc2).close();
    DataSubscriber<Object> subscriber3 = verifyGetAndSubscribe(mDataSourceSupplier3, mSrc3);
    verifyState(dataSource, null, NOT_CLOSED, NOT_FINISHED, WITHOUT_RESULT, null, NOT_FAILED, null);

    Throwable throwable = mock(Throwable.class);
    setState(mSrc3, NOT_CLOSED, FINISHED, WITHOUT_RESULT, null, FAILED, throwable);
    subscriber3.onFailure(mSrc3);
    mInOrder.verify(mSrc3).close();
    verifySubscriber(dataSource, mSrc3, ON_FAILURE);
    verifyState(dataSource, null, NOT_CLOSED, FINISHED, WITHOUT_RESULT, null, FAILED, throwable);

    testClose(dataSource);
    verifyState(dataSource, null, CLOSED, FINISHED, WITHOUT_RESULT, null, FAILED, throwable);
  }

  /**
   * All data sources failed, second data source produced multiple intermediate results.
   */
  @Test
  public void testLifecycle_F1_I2_I2_F2_F3_C() {
    DataSource<Object> dataSource = getAndSubscribe();
    DataSubscriber<Object> subscriber1 = verifyGetAndSubscribe(mDataSourceSupplier1, mSrc1);

    setState(mSrc1, NOT_CLOSED, FINISHED, WITHOUT_RESULT, null, FAILED, mock(Throwable.class));
    subscriber1.onFailure(mSrc1);
    mInOrder.verify(mSrc1).close();
    DataSubscriber<Object> subscriber2 = verifyGetAndSubscribe(mDataSourceSupplier2, mSrc2);
    verifyState(dataSource, null, NOT_CLOSED, NOT_FINISHED, WITHOUT_RESULT, null, NOT_FAILED, null);

    Object val2a = mock(Object.class);
    setState(mSrc2, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val2a, NOT_FAILED, null);
    subscriber2.onNewResult(mSrc2);
    verifySubscriber(dataSource, mSrc2, ON_NEW_RESULT);
    verifyState(dataSource, mSrc2, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val2a, NOT_FAILED, null);

    Object val2b = mock(Object.class);
    setState(mSrc2, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val2b, NOT_FAILED, null);
    subscriber2.onNewResult(mSrc2);
    verifySubscriber(dataSource, mSrc2, ON_NEW_RESULT);
    verifyState(dataSource, mSrc2, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val2b, NOT_FAILED, null);

    setState(mSrc2, NOT_CLOSED, FINISHED, WITH_RESULT, val2b, FAILED, mock(Throwable.class));
    subscriber2.onFailure(mSrc2);
    DataSubscriber<Object> subscriber3 = verifyGetAndSubscribe(mDataSourceSupplier3, mSrc3);
    verifyState(dataSource, mSrc2, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val2b, NOT_FAILED, null);

    Throwable throwable = mock(Throwable.class);
    setState(mSrc3, NOT_CLOSED, FINISHED, WITHOUT_RESULT, null, FAILED, throwable);
    subscriber3.onFailure(mSrc3);
    mInOrder.verify(mSrc3).close();
    verifySubscriber(dataSource, mSrc3, ON_FAILURE);
    verifyState(dataSource, mSrc2, NOT_CLOSED, FINISHED, WITH_RESULT, val2b, FAILED, throwable);

    testClose(dataSource, mSrc2);
    verifyState(dataSource, null, CLOSED, FINISHED, WITHOUT_RESULT, null, FAILED, throwable);
  }

  /**
   * All data sources failed, first two data sources produced intermediate results. Only first kept.
   */
  @Test
  public void testLifecycle_I1_F1_I2_F2_F3_C() {
    DataSource<Object> dataSource = getAndSubscribe();
    DataSubscriber<Object> subscriber1 = verifyGetAndSubscribe(mDataSourceSupplier1, mSrc1);

    Object val1 = mock(Object.class);
    setState(mSrc1, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val1, NOT_FAILED, null);
    subscriber1.onNewResult(mSrc1);
    verifySubscriber(dataSource, mSrc1, ON_NEW_RESULT);
    verifyState(dataSource, mSrc1, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val1, NOT_FAILED, null);

    setState(mSrc1, NOT_CLOSED, FINISHED, WITH_RESULT, val1, FAILED, mock(Throwable.class));
    subscriber1.onFailure(mSrc1);
    DataSubscriber<Object> subscriber2 = verifyGetAndSubscribe(mDataSourceSupplier2, mSrc2);
    verifyState(dataSource, mSrc1, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val1, NOT_FAILED, null);

    // I2 gets ignored because we already have I1
    Object val2 = mock(Object.class);
    setState(mSrc2, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val2, NOT_FAILED, null);
    subscriber2.onNewResult(mSrc2);
    verifySubscriber(dataSource, mSrc2, NO_INTERACTIONS);
    verifyState(dataSource, mSrc1, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val1, NOT_FAILED, null);

    setState(mSrc2, NOT_CLOSED, FINISHED, WITH_RESULT, val2, FAILED, mock(Throwable.class));
    subscriber2.onFailure(mSrc2);
    mInOrder.verify(mSrc2).close();
    DataSubscriber<Object> subscriber3 = verifyGetAndSubscribe(mDataSourceSupplier3, mSrc3);
    verifyState(dataSource, mSrc1, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val1, NOT_FAILED, null);

    Throwable throwable = mock(Throwable.class);
    setState(mSrc3, NOT_CLOSED, FINISHED, WITHOUT_RESULT, null, FAILED, throwable);
    subscriber3.onFailure(mSrc3);
    mInOrder.verify(mSrc3).close();
    verifySubscriber(dataSource, mSrc3, ON_FAILURE);
    verifyState(dataSource, mSrc1, NOT_CLOSED, FINISHED, WITH_RESULT, val1, FAILED, throwable);

    testClose(dataSource, mSrc1);
    verifyState(dataSource, null, CLOSED, FINISHED, WITHOUT_RESULT, null, FAILED, throwable);
  }

  /**
   * First data source failed, second succeeded, no intermediate results.
   */
  @Test
  public void testLifecycle_F1_S2_C() {
    DataSource<Object> dataSource = getAndSubscribe();
    DataSubscriber<Object> subscriber1 = verifyGetAndSubscribe(mDataSourceSupplier1, mSrc1);

    setState(mSrc1, NOT_CLOSED, FINISHED, WITHOUT_RESULT, null, FAILED, mock(Throwable.class));
    subscriber1.onFailure(mSrc1);
    mInOrder.verify(mSrc1).close();
    DataSubscriber<Object> subscriber2 = verifyGetAndSubscribe(mDataSourceSupplier2, mSrc2);
    verifyState(dataSource, null, NOT_CLOSED, NOT_FINISHED, WITHOUT_RESULT, null, NOT_FAILED, null);

    Object val = mock(Object.class);
    setState(mSrc2, NOT_CLOSED, FINISHED, WITH_RESULT, val, NOT_FAILED, null);
    subscriber2.onNewResult(mSrc2);
    verifySubscriber(dataSource, mSrc2, ON_NEW_RESULT);
    verifyState(dataSource, mSrc2, NOT_CLOSED, FINISHED, WITH_RESULT, val, NOT_FAILED, null);

    testClose(dataSource, mSrc2);
    verifyState(dataSource, null, CLOSED, FINISHED, WITHOUT_RESULT, null, NOT_FAILED, null);
  }

  /**
   * First data source succeeded, no intermediate results.
   */
  @Test
  public void testLifecycle_S1_C() {
    DataSource<Object> dataSource = getAndSubscribe();
    DataSubscriber<Object> subscriber1 = verifyGetAndSubscribe(mDataSourceSupplier1, mSrc1);

    Object val = mock(Object.class);
    setState(mSrc1, NOT_CLOSED, FINISHED, WITH_RESULT, val, NOT_FAILED, null);
    subscriber1.onNewResult(mSrc1);
    verifySubscriber(dataSource, mSrc1, ON_NEW_RESULT);
    verifyState(dataSource, mSrc1, NOT_CLOSED, FINISHED, WITH_RESULT, val, NOT_FAILED, null);

    testClose(dataSource, mSrc1);
    verifyState(dataSource, null, CLOSED, FINISHED, WITHOUT_RESULT, null, NOT_FAILED, null);
  }

  /**
   * First data source succeeded, with multiple intermediate results.
   */
  @Test
  public void testLifecycle_I1_I1_S1_C() {
    DataSource<Object> dataSource = getAndSubscribe();
    DataSubscriber<Object> subscriber1 = verifyGetAndSubscribe(mDataSourceSupplier1, mSrc1);

    Object val1 = mock(Object.class);
    setState(mSrc1, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val1, NOT_FAILED, null);
    subscriber1.onNewResult(mSrc1);
    verifySubscriber(dataSource, mSrc1, ON_NEW_RESULT);
    verifyState(dataSource, mSrc1, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val1, NOT_FAILED, null);

    Object val2 = mock(Object.class);
    setState(mSrc1, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val2, NOT_FAILED, null);
    subscriber1.onNewResult(mSrc1);
    verifySubscriber(dataSource, mSrc1, ON_NEW_RESULT);
    verifyState(dataSource, mSrc1, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val2, NOT_FAILED, null);

    Object val = mock(Object.class);
    setState(mSrc1, NOT_CLOSED, FINISHED, WITH_RESULT, val, NOT_FAILED, null);
    subscriber1.onNewResult(mSrc1);
    verifySubscriber(dataSource, mSrc1, ON_NEW_RESULT);
    verifyState(dataSource, mSrc1, NOT_CLOSED, FINISHED, WITH_RESULT, val, NOT_FAILED, null);

    testClose(dataSource, mSrc1);
    verifyState(dataSource, null, CLOSED, FINISHED, WITHOUT_RESULT, null, NOT_FAILED, null);
  }

  /**
   * First data source failed with intermediate results, second succeeded with intermediate results.
   */
  @Test
  public void testLifecycle_I1_F1_I2_S2_C() {
    DataSource<Object> dataSource = getAndSubscribe();
    DataSubscriber<Object> subscriber1 = verifyGetAndSubscribe(mDataSourceSupplier1, mSrc1);

    Object val1 = mock(Object.class);
    setState(mSrc1, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val1, NOT_FAILED, null);
    subscriber1.onNewResult(mSrc1);
    verifySubscriber(dataSource, mSrc1, ON_NEW_RESULT);
    verifyState(dataSource, mSrc1, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val1, NOT_FAILED, null);

    setState(mSrc1, NOT_CLOSED, FINISHED, WITH_RESULT, val1, FAILED, mock(Throwable.class));
    subscriber1.onFailure(mSrc1);
    DataSubscriber<Object> subscriber2 = verifyGetAndSubscribe(mDataSourceSupplier2, mSrc2);
    verifyState(dataSource, mSrc1, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val1, NOT_FAILED, null);

    // I2 gets ignored because we already have I1
    Object val2 = mock(Object.class);
    setState(mSrc2, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val2, NOT_FAILED, null);
    subscriber2.onNewResult(mSrc2);
    verifySubscriber(dataSource, mSrc2, NO_INTERACTIONS);
    verifyState(dataSource, mSrc1, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val1, NOT_FAILED, null);

    Object val = mock(Object.class);
    setState(mSrc2, NOT_CLOSED, FINISHED, WITH_RESULT, val, NOT_FAILED, null);
    subscriber2.onNewResult(mSrc2);
    mInOrder.verify(mSrc1).close();
    verifySubscriber(dataSource, mSrc2, ON_NEW_RESULT);
    verifyState(dataSource, mSrc2, NOT_CLOSED, FINISHED, WITH_RESULT, val, NOT_FAILED, null);

    testClose(dataSource, mSrc2);
    verifyState(dataSource, null, CLOSED, FINISHED, WITHOUT_RESULT, null, NOT_FAILED, null);
  }

  /**
   * First data source failed with intermediate results, second had intermediate results but closed.
   */
  @Test
  public void testLifecycle_I1_F1_I2_C() {
    DataSource<Object> dataSource = getAndSubscribe();
    DataSubscriber<Object> subscriber1 = verifyGetAndSubscribe(mDataSourceSupplier1, mSrc1);

    Object val1 = mock(Object.class);
    setState(mSrc1, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val1, NOT_FAILED, null);
    subscriber1.onNewResult(mSrc1);
    verifySubscriber(dataSource, mSrc1, ON_NEW_RESULT);
    verifyState(dataSource, mSrc1, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val1, NOT_FAILED, null);

    setState(mSrc1, NOT_CLOSED, FINISHED, WITH_RESULT, val1, FAILED, mock(Throwable.class));
    subscriber1.onFailure(mSrc1);
    DataSubscriber<Object> subscriber2 = verifyGetAndSubscribe(mDataSourceSupplier2, mSrc2);
    verifyState(dataSource, mSrc1, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val1, NOT_FAILED, null);

    // I2 gets ignored because we already have I1
    Object val2 = mock(Object.class);
    setState(mSrc2, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val2, NOT_FAILED, null);
    subscriber2.onNewResult(mSrc2);
    verifySubscriber(dataSource, mSrc2, NO_INTERACTIONS);
    verifyState(dataSource, mSrc1, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val1, NOT_FAILED, null);

    testClose(dataSource, mSrc1, mSrc2);
    verifySubscriber(dataSource, null, ON_CANCELLATION);
    verifyState(dataSource, null, CLOSED, NOT_FINISHED, WITHOUT_RESULT, null, NOT_FAILED, null);
  }

  /**
   * Early close with no results.
   */
  @Test
  public void testLifecycle_C() {
    DataSource<Object> dataSource = getAndSubscribe();
    DataSubscriber<Object> subscriber1 = verifyGetAndSubscribe(mDataSourceSupplier1, mSrc1);

    testClose(dataSource, mSrc1);
    verifySubscriber(dataSource, null, ON_CANCELLATION);
    verifyState(dataSource, null, CLOSED, NOT_FINISHED, WITHOUT_RESULT, null, NOT_FAILED, null);
  }

  /**
   * Ignore callbacks after closed.
   */
  @Test
  public void testLifecycle_I1_C_S1() {
    DataSource<Object> dataSource = getAndSubscribe();
    DataSubscriber<Object> subscriber1 = verifyGetAndSubscribe(mDataSourceSupplier1, mSrc1);

    Object val1 = mock(Object.class);
    setState(mSrc1, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val1, NOT_FAILED, null);
    subscriber1.onNewResult(mSrc1);
    verifySubscriber(dataSource, mSrc1, ON_NEW_RESULT);
    verifyState(dataSource, mSrc1, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val1, NOT_FAILED, null);

    testClose(dataSource, mSrc1);
    verifySubscriber(dataSource, null, ON_CANCELLATION);
    verifyState(dataSource, null, CLOSED, NOT_FINISHED, WITHOUT_RESULT, null, NOT_FAILED, null);

    Object val = mock(Object.class);
    setState(mSrc1, NOT_CLOSED, FINISHED, WITH_RESULT, val, NOT_FAILED, null);
    subscriber1.onNewResult(mSrc1);
    verifySubscriber(dataSource, mSrc1, NO_INTERACTIONS);
    verifyState(dataSource, null, CLOSED, NOT_FINISHED, WITHOUT_RESULT, null, NOT_FAILED, null);
  }

  /**
   * Test data source without result
   */
  @Test
  public void testLifecycle_WithoutResult_NI1_NS1_I2_S2_C() {
    DataSource<Object> dataSource = getAndSubscribe();
    DataSubscriber<Object> subscriber1 = verifyGetAndSubscribe(mDataSourceSupplier1, mSrc1);

    // I1 gets ignored because there is no result
    setState(mSrc1, NOT_CLOSED, NOT_FINISHED, WITHOUT_RESULT, null, NOT_FAILED, null);
    subscriber1.onNewResult(mSrc1);
    verifySubscriber(dataSource, mSrc1, NO_INTERACTIONS);
    verifyState(dataSource, null, NOT_CLOSED, NOT_FINISHED, WITHOUT_RESULT, null, NOT_FAILED, null);

    // S1 gets ignored because there is no result
    setState(mSrc1, NOT_CLOSED, FINISHED, WITHOUT_RESULT, null, NOT_FAILED, null);
    subscriber1.onNewResult(mSrc1);
    mInOrder.verify(mSrc1).close();
    DataSubscriber<Object> subscriber2 = verifyGetAndSubscribe(mDataSourceSupplier2, mSrc2);
    verifyState(dataSource, null, NOT_CLOSED, NOT_FINISHED, WITHOUT_RESULT, null, NOT_FAILED, null);

    Object val2a = mock(Object.class);
    setState(mSrc2, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val2a, NOT_FAILED, null);
    subscriber2.onNewResult(mSrc2);
    verifySubscriber(dataSource, mSrc2, ON_NEW_RESULT);
    verifyState(dataSource, mSrc2, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val2a, NOT_FAILED, null);

    Object val2b = mock(Object.class);
    setState(mSrc2, NOT_CLOSED, FINISHED, WITH_RESULT, val2b, NOT_FAILED, null);
    subscriber2.onNewResult(mSrc2);
    verifySubscriber(dataSource, mSrc2, ON_NEW_RESULT);
    verifyState(dataSource, mSrc2, NOT_CLOSED, FINISHED, WITH_RESULT, val2b, NOT_FAILED, null);

    testClose(dataSource, mSrc2);
    verifyState(dataSource, null, CLOSED, FINISHED, WITHOUT_RESULT, null, NOT_FAILED, null);
  }
}

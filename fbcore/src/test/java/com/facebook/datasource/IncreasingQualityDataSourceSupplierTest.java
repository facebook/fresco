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
 * Tests for IncreasingQualityDataSourceSupplier
 */
@RunWith(RobolectricTestRunner.class)
public class IncreasingQualityDataSourceSupplierTest extends AbstractDataSourceSupplier {

  @Before
  public void setUp() {
    super.setUp();
    mDataSourceSupplier = IncreasingQualityDataSourceSupplier.create(mSuppliers);
  }

  /**
   * All data sources failed, highest-quality failed last, no intermediate results.
   */
  @Test
  public void testLifecycle_F2_F3_F1_C() {
    DataSource<Object> dataSource = getAndSubscribe();
    DataSubscriber<Object> subscriber1 = verifyGetAndSubscribeM(mDataSourceSupplier1, mSrc1);
    DataSubscriber<Object> subscriber2 = verifyGetAndSubscribeM(mDataSourceSupplier2, mSrc2);
    DataSubscriber<Object> subscriber3 = verifyGetAndSubscribe(mDataSourceSupplier3, mSrc3);

    setState(mSrc2, NOT_CLOSED, FINISHED, WITHOUT_RESULT, null, FAILED, mock(Throwable.class));
    subscriber2.onFailure(mSrc2);
    mInOrder.verify(mSrc2).close();
    verifySubscriber(dataSource, mSrc2, NO_INTERACTIONS);
    verifyState(dataSource, null, NOT_CLOSED, NOT_FINISHED, WITHOUT_RESULT, null, NOT_FAILED, null);

    setState(mSrc3, NOT_CLOSED, FINISHED, WITHOUT_RESULT, null, FAILED, mock(Throwable.class));
    subscriber3.onFailure(mSrc3);
    mInOrder.verify(mSrc3).close();
    verifySubscriber(dataSource, mSrc3, NO_INTERACTIONS);
    verifyState(dataSource, null, NOT_CLOSED, NOT_FINISHED, WITHOUT_RESULT, null, NOT_FAILED, null);

    Throwable throwable = mock(Throwable.class);
    setState(mSrc1, NOT_CLOSED, FINISHED, WITHOUT_RESULT, null, FAILED, throwable);
    subscriber1.onFailure(mSrc1);
    mInOrder.verify(mSrc1).close();
    verifySubscriber(dataSource, mSrc1, ON_FAILURE);
    verifyState(dataSource, null, NOT_CLOSED, FINISHED, WITHOUT_RESULT, null, FAILED, throwable);

    testClose(dataSource);
    verifyState(dataSource, null, CLOSED, FINISHED, WITHOUT_RESULT, null, FAILED, throwable);
  }

  /**
   * Highest-quality data source failed second, result of the third data source is ignored.
   */
  @Test
  public void testLifecycle_F2_F1_S3_C() {
    DataSource<Object> dataSource = getAndSubscribe();
    DataSubscriber<Object> subscriber1 = verifyGetAndSubscribeM(mDataSourceSupplier1, mSrc1);
    DataSubscriber<Object> subscriber2 = verifyGetAndSubscribeM(mDataSourceSupplier2, mSrc2);
    DataSubscriber<Object> subscriber3 = verifyGetAndSubscribe(mDataSourceSupplier3, mSrc3);

    setState(mSrc2, NOT_CLOSED, FINISHED, WITHOUT_RESULT, null, FAILED, mock(Throwable.class));
    subscriber2.onFailure(mSrc2);
    mInOrder.verify(mSrc2).close();
    verifySubscriber(dataSource, mSrc2, NO_INTERACTIONS);
    verifyState(dataSource, null, NOT_CLOSED, NOT_FINISHED, WITHOUT_RESULT, null, NOT_FAILED, null);

    Throwable throwable = mock(Throwable.class);
    setState(mSrc1, NOT_CLOSED, FINISHED, WITHOUT_RESULT, null, FAILED, throwable);
    subscriber1.onFailure(mSrc1);
    mInOrder.verify(mSrc1).close();
    verifySubscriber(dataSource, mSrc1, ON_FAILURE);
    verifyState(dataSource, null, NOT_CLOSED, FINISHED, WITHOUT_RESULT, null, FAILED, throwable);

    // gets ignored because DS1 failed
    setState(mSrc3, NOT_CLOSED, FINISHED, WITH_RESULT, mock(Object.class), NOT_FAILED, null);
    subscriber3.onFailure(mSrc3);
    mInOrder.verify(mSrc3).close();
    verifySubscriber(dataSource, mSrc3, NO_INTERACTIONS);
    verifyState(dataSource, null, NOT_CLOSED, FINISHED, WITHOUT_RESULT, null, FAILED, throwable);

    testClose(dataSource);
    verifyState(dataSource, null, CLOSED, FINISHED, WITHOUT_RESULT, null, FAILED, throwable);
  }

  /**
   * Highest-quality data source failed, result of the third data source is ignored.
   * Second data source produced intermediate result first, the result is preserved until closed.
   */
  @Test
  public void testLifecycle_I2_F2_F1_S3_C() {
    DataSource<Object> dataSource = getAndSubscribe();
    DataSubscriber<Object> subscriber1 = verifyGetAndSubscribeM(mDataSourceSupplier1, mSrc1);
    DataSubscriber<Object> subscriber2 = verifyGetAndSubscribeM(mDataSourceSupplier2, mSrc2);
    DataSubscriber<Object> subscriber3 = verifyGetAndSubscribe(mDataSourceSupplier3, mSrc3);

    Object val2 = mock(Object.class);
    setState(mSrc2, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val2, NOT_FAILED, null);
    subscriber2.onNewResult(mSrc2);
    mInOrder.verify(mSrc3).close();
    verifySubscriber(dataSource, mSrc2, ON_NEW_RESULT);
    verifyState(dataSource, mSrc2, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val2, NOT_FAILED, null);

    setState(mSrc2, NOT_CLOSED, FINISHED, WITH_RESULT, val2, FAILED, mock(Throwable.class));
    subscriber2.onFailure(mSrc2);
    verifySubscriber(dataSource, mSrc2, NO_INTERACTIONS);
    verifyState(dataSource, mSrc2, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val2, NOT_FAILED, null);

    Throwable throwable = mock(Throwable.class);
    setState(mSrc1, NOT_CLOSED, FINISHED, WITHOUT_RESULT, null, FAILED, throwable);
    subscriber1.onFailure(mSrc1);
    mInOrder.verify(mSrc1).close();
    verifySubscriber(dataSource, mSrc1, ON_FAILURE);
    verifyState(dataSource, mSrc2, NOT_CLOSED, FINISHED, WITH_RESULT, val2, FAILED, throwable);

    // gets ignored because DS1 failed
    // besides, this data source shouldn't have finished as it was supposed to be closed!
    setState(mSrc3, NOT_CLOSED, FINISHED, WITH_RESULT, mock(Object.class), NOT_FAILED, null);
    subscriber3.onFailure(mSrc3);
    mInOrder.verify(mSrc3).close();
    verifySubscriber(dataSource, mSrc3, NO_INTERACTIONS);
    verifyState(dataSource, mSrc2, NOT_CLOSED, FINISHED, WITH_RESULT, val2, FAILED, throwable);

    testClose(dataSource, mSrc2);
    verifyState(dataSource, null, CLOSED, FINISHED, WITHOUT_RESULT, null, FAILED, throwable);
  }

  /**
   * Second data source produced multiple intermediate results first, intermediate result of
   * highest-quality data source gets ignored afterwards. Second data source fails and first data
   * source produced another intermediate result, but it gets ignored again. Finally, first data
   * source produced its final result which is set.
   */
  @Test
  public void testLifecycle_I2_I2_I1_F2_I1_S1_C() {
    DataSource<Object> dataSource = getAndSubscribe();
    DataSubscriber<Object> subscriber1 = verifyGetAndSubscribeM(mDataSourceSupplier1, mSrc1);
    DataSubscriber<Object> subscriber2 = verifyGetAndSubscribeM(mDataSourceSupplier2, mSrc2);
    DataSubscriber<Object> subscriber3 = verifyGetAndSubscribe(mDataSourceSupplier3, mSrc3);

    Object val2a = mock(Object.class);
    setState(mSrc2, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val2a, NOT_FAILED, null);
    subscriber2.onNewResult(mSrc2);
    mInOrder.verify(mSrc3).close();
    verifySubscriber(dataSource, mSrc2, ON_NEW_RESULT);
    verifyState(dataSource, mSrc2, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val2a, NOT_FAILED, null);

    Object val2b = mock(Object.class);
    setState(mSrc2, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val2b, NOT_FAILED, null);
    subscriber2.onNewResult(mSrc2);
    verifySubscriber(dataSource, mSrc2, ON_NEW_RESULT);
    verifyState(dataSource, mSrc2, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val2b, NOT_FAILED, null);

    // gets ignored because DS2 was first to produce result
    Object val1a = mock(Object.class);
    setState(mSrc1, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val1a, NOT_FAILED, null);
    subscriber1.onNewResult(mSrc1);
    verifySubscriber(dataSource, mSrc1, NO_INTERACTIONS);
    verifyState(dataSource, mSrc2, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val2b, NOT_FAILED, null);

    setState(mSrc2, NOT_CLOSED, FINISHED, WITH_RESULT, val2b, FAILED, mock(Throwable.class));
    subscriber2.onFailure(mSrc2);
    verifySubscriber(dataSource, mSrc2, NO_INTERACTIONS);
    verifyState(dataSource, mSrc2, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val2b, NOT_FAILED, null);

    // gets ignored because DS2 was first to produce result
    Object val1b = mock(Object.class);
    setState(mSrc1, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val1b, NOT_FAILED, null);
    subscriber1.onNewResult(mSrc1);
    verifySubscriber(dataSource, mSrc1, NO_INTERACTIONS);
    verifyState(dataSource, mSrc2, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val2b, NOT_FAILED, null);

    Object val1c = mock(Object.class);
    setState(mSrc1, NOT_CLOSED, FINISHED, WITH_RESULT, val1c, NOT_FAILED, null);
    subscriber1.onNewResult(mSrc1);
    mInOrder.verify(mSrc2).close();
    verifySubscriber(dataSource, mSrc1, ON_NEW_RESULT);
    verifyState(dataSource, mSrc1, NOT_CLOSED, FINISHED, WITH_RESULT, val1c, NOT_FAILED, null);

    testClose(dataSource, mSrc1);
    verifyState(dataSource, null, CLOSED, FINISHED, WITHOUT_RESULT, null, NOT_FAILED, null);
  }

  /**
   * Interleaved results.
   */
  @Test
  public void testLifecycle_I3_I2_I3_S2_I1_S1_C() {
    DataSource<Object> dataSource = getAndSubscribe();
    DataSubscriber<Object> subscriber1 = verifyGetAndSubscribeM(mDataSourceSupplier1, mSrc1);
    DataSubscriber<Object> subscriber2 = verifyGetAndSubscribeM(mDataSourceSupplier2, mSrc2);
    DataSubscriber<Object> subscriber3 = verifyGetAndSubscribe(mDataSourceSupplier3, mSrc3);

    Object val3a = mock(Object.class);
    setState(mSrc3, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val3a, NOT_FAILED, null);
    subscriber3.onNewResult(mSrc3);
    verifySubscriber(dataSource, mSrc3, ON_NEW_RESULT);
    verifyState(dataSource, mSrc3, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val3a, NOT_FAILED, null);

    // gets ignored because DS3 was first
    Object val2a = mock(Object.class);
    setState(mSrc2, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val2a, NOT_FAILED, null);
    subscriber2.onNewResult(mSrc2);
    verifySubscriber(dataSource, mSrc2, NO_INTERACTIONS);
    verifyState(dataSource, mSrc3, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val3a, NOT_FAILED, null);

    Object val3b = mock(Object.class);
    setState(mSrc3, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val3b, NOT_FAILED, null);
    subscriber3.onNewResult(mSrc3);
    verifySubscriber(dataSource, mSrc3, ON_NEW_RESULT);
    verifyState(dataSource, mSrc3, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val3b, NOT_FAILED, null);

    Object val2b = mock(Object.class);
    setState(mSrc2, NOT_CLOSED, FINISHED, WITH_RESULT, val2b, NOT_FAILED, null);
    subscriber2.onNewResult(mSrc2);
    mInOrder.verify(mSrc3).close();
    verifySubscriber(dataSource, mSrc2, ON_NEW_RESULT);
    verifyState(dataSource, mSrc2, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val2b, NOT_FAILED, null);

    // gets ignored because DS2 was first
    Object val1a = mock(Object.class);
    setState(mSrc1, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val1a, NOT_FAILED, null);
    subscriber1.onNewResult(mSrc1);
    verifySubscriber(dataSource, mSrc1, NO_INTERACTIONS);
    verifyState(dataSource, mSrc2, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val2b, NOT_FAILED, null);

    Object val1b = mock(Object.class);
    setState(mSrc1, NOT_CLOSED, FINISHED, WITH_RESULT, val1b, NOT_FAILED, null);
    subscriber1.onNewResult(mSrc1);
    mInOrder.verify(mSrc2).close();
    verifySubscriber(dataSource, mSrc1, ON_NEW_RESULT);
    verifyState(dataSource, mSrc1, NOT_CLOSED, FINISHED, WITH_RESULT, val1b, NOT_FAILED, null);

    testClose(dataSource, mSrc1);
    verifyState(dataSource, null, CLOSED, FINISHED, WITHOUT_RESULT, null, NOT_FAILED, null);
  }

  /**
   * Second data source produced its final result, followed by the first data source.
   */
  @Test
  public void testLifecycle_S2_S1_C() {
    DataSource<Object> dataSource = getAndSubscribe();
    DataSubscriber<Object> subscriber1 = verifyGetAndSubscribeM(mDataSourceSupplier1, mSrc1);
    DataSubscriber<Object> subscriber2 = verifyGetAndSubscribeM(mDataSourceSupplier2, mSrc2);
    DataSubscriber<Object> subscriber3 = verifyGetAndSubscribe(mDataSourceSupplier3, mSrc3);

    Object val2 = mock(Object.class);
    setState(mSrc2, NOT_CLOSED, FINISHED, WITH_RESULT, val2, NOT_FAILED, null);
    subscriber2.onNewResult(mSrc2);
    mInOrder.verify(mSrc3).close();
    verifySubscriber(dataSource, mSrc2, ON_NEW_RESULT);
    verifyState(dataSource, mSrc2, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val2, NOT_FAILED, null);

    Object val1 = mock(Object.class);
    setState(mSrc1, NOT_CLOSED, FINISHED, WITH_RESULT, val1, NOT_FAILED, null);
    subscriber1.onNewResult(mSrc1);
    mInOrder.verify(mSrc2).close();
    verifySubscriber(dataSource, mSrc1, ON_NEW_RESULT);
    verifyState(dataSource, mSrc1, NOT_CLOSED, FINISHED, WITH_RESULT, val1, NOT_FAILED, null);

    testClose(dataSource, mSrc1);
    verifyState(dataSource, null, CLOSED, FINISHED, WITHOUT_RESULT, null, NOT_FAILED, null);
  }

  /**
   * Highest-quality data source was first to produce result, other data sources got closed.
   */
  @Test
  public void testLifecycle_I1_S1_C() {
    DataSource<Object> dataSource = getAndSubscribe();
    DataSubscriber<Object> subscriber1 = verifyGetAndSubscribeM(mDataSourceSupplier1, mSrc1);
    DataSubscriber<Object> subscriber2 = verifyGetAndSubscribeM(mDataSourceSupplier2, mSrc2);
    DataSubscriber<Object> subscriber3 = verifyGetAndSubscribe(mDataSourceSupplier3, mSrc3);

    Object val1a = mock(Object.class);
    setState(mSrc1, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val1a, NOT_FAILED, null);
    subscriber1.onNewResult(mSrc1);
    mInOrder.verify(mSrc3).close();
    mInOrder.verify(mSrc2).close();
    verifySubscriber(dataSource, mSrc1, ON_NEW_RESULT);
    verifyState(dataSource, mSrc1, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val1a, NOT_FAILED, null);

    Object val1b = mock(Object.class);
    setState(mSrc1, NOT_CLOSED, FINISHED, WITH_RESULT, val1b, NOT_FAILED, null);
    subscriber1.onNewResult(mSrc1);
    verifySubscriber(dataSource, mSrc1, ON_NEW_RESULT);
    verifyState(dataSource, mSrc1, NOT_CLOSED, FINISHED, WITH_RESULT, val1b, NOT_FAILED, null);

    testClose(dataSource, mSrc1);
    verifyState(dataSource, null, CLOSED, FINISHED, WITHOUT_RESULT, null, NOT_FAILED, null);
  }

  /**
   * Highest-quality data source was first to produce result, other data sources got closed.
   */
  @Test
  public void testLifecycle_S1_C() {
    DataSource<Object> dataSource = getAndSubscribe();
    DataSubscriber<Object> subscriber1 = verifyGetAndSubscribeM(mDataSourceSupplier1, mSrc1);
    DataSubscriber<Object> subscriber2 = verifyGetAndSubscribeM(mDataSourceSupplier2, mSrc2);
    DataSubscriber<Object> subscriber3 = verifyGetAndSubscribe(mDataSourceSupplier3, mSrc3);

    Object val1b = mock(Object.class);
    setState(mSrc1, NOT_CLOSED, FINISHED, WITH_RESULT, val1b, NOT_FAILED, null);
    subscriber1.onNewResult(mSrc1);
    mInOrder.verify(mSrc3).close();
    mInOrder.verify(mSrc2).close();
    verifySubscriber(dataSource, mSrc1, ON_NEW_RESULT);
    verifyState(dataSource, mSrc1, NOT_CLOSED, FINISHED, WITH_RESULT, val1b, NOT_FAILED, null);

    testClose(dataSource, mSrc1);
    verifyState(dataSource, null, CLOSED, FINISHED, WITHOUT_RESULT, null, NOT_FAILED, null);
  }

  /**
   * Early close with intermediate result.
   */
  @Test
  public void testLifecycle_I2_C() {
    DataSource<Object> dataSource = getAndSubscribe();
    DataSubscriber<Object> subscriber1 = verifyGetAndSubscribeM(mDataSourceSupplier1, mSrc1);
    DataSubscriber<Object> subscriber2 = verifyGetAndSubscribeM(mDataSourceSupplier2, mSrc2);
    DataSubscriber<Object> subscriber3 = verifyGetAndSubscribe(mDataSourceSupplier3, mSrc3);

    Object val2a = mock(Object.class);
    setState(mSrc2, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val2a, NOT_FAILED, null);
    subscriber2.onNewResult(mSrc2);
    mInOrder.verify(mSrc3).close();
    verifySubscriber(dataSource, mSrc2, ON_NEW_RESULT);
    verifyState(dataSource, mSrc2, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val2a, NOT_FAILED, null);

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
    DataSubscriber<Object> subscriber1 = verifyGetAndSubscribeM(mDataSourceSupplier1, mSrc1);
    DataSubscriber<Object> subscriber2 = verifyGetAndSubscribeM(mDataSourceSupplier2, mSrc2);
    DataSubscriber<Object> subscriber3 = verifyGetAndSubscribe(mDataSourceSupplier3, mSrc3);

    testClose(dataSource, mSrc1, mSrc2, mSrc3);
    verifySubscriber(dataSource, null, ON_CANCELLATION);
    verifyState(dataSource, null, CLOSED, NOT_FINISHED, WITHOUT_RESULT, null, NOT_FAILED, null);
  }

  /**
   * Ignore callbacks after closed.
   */
  @Test
  public void testLifecycle_I2_C_S1() {
    DataSource<Object> dataSource = getAndSubscribe();
    DataSubscriber<Object> subscriber1 = verifyGetAndSubscribeM(mDataSourceSupplier1, mSrc1);
    DataSubscriber<Object> subscriber2 = verifyGetAndSubscribeM(mDataSourceSupplier2, mSrc2);
    DataSubscriber<Object> subscriber3 = verifyGetAndSubscribe(mDataSourceSupplier3, mSrc3);

    Object val2a = mock(Object.class);
    setState(mSrc2, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val2a, NOT_FAILED, null);
    subscriber2.onNewResult(mSrc2);
    mInOrder.verify(mSrc3).close();
    verifySubscriber(dataSource, mSrc2, ON_NEW_RESULT);
    verifyState(dataSource, mSrc2, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val2a, NOT_FAILED, null);

    testClose(dataSource, mSrc1, mSrc2);
    verifySubscriber(dataSource, null, ON_CANCELLATION);
    verifyState(dataSource, null, CLOSED, NOT_FINISHED, WITHOUT_RESULT, null, NOT_FAILED, null);

    Object val = mock(Object.class);
    setState(mSrc2, NOT_CLOSED, FINISHED, WITH_RESULT, val, NOT_FAILED, null);
    subscriber2.onNewResult(mSrc2);
    verifySubscriber(dataSource, mSrc2, NO_INTERACTIONS);
    verifyState(dataSource, null, CLOSED, NOT_FINISHED, WITHOUT_RESULT, null, NOT_FAILED, null);
  }

  /**
   * Test data source without result
   */
  @Test
  public void testLifecycle_WithoutResult_NI2_NS2_I3_S3_S1_C() {
    DataSource<Object> dataSource = getAndSubscribe();
    DataSubscriber<Object> subscriber1 = verifyGetAndSubscribeM(mDataSourceSupplier1, mSrc1);
    DataSubscriber<Object> subscriber2 = verifyGetAndSubscribeM(mDataSourceSupplier2, mSrc2);
    DataSubscriber<Object> subscriber3 = verifyGetAndSubscribe(mDataSourceSupplier3, mSrc3);

    // I2 gets ignored because there is no result
    setState(mSrc2, NOT_CLOSED, NOT_FINISHED, WITHOUT_RESULT, null, NOT_FAILED, null);
    subscriber2.onNewResult(mSrc2);
    verifySubscriber(dataSource, mSrc2, NO_INTERACTIONS);
    verifyState(dataSource, null, NOT_CLOSED, NOT_FINISHED, WITHOUT_RESULT, null, NOT_FAILED, null);

    // S2 gets ignored because there is no result
    setState(mSrc2, NOT_CLOSED, FINISHED, WITHOUT_RESULT, null, NOT_FAILED, null);
    subscriber2.onNewResult(mSrc2);
    mInOrder.verify(mSrc2).close();
    verifySubscriber(dataSource, mSrc2, NO_INTERACTIONS);
    verifyState(dataSource, null, NOT_CLOSED, NOT_FINISHED, WITHOUT_RESULT, null, NOT_FAILED, null);

    Object val3a = mock(Object.class);
    setState(mSrc3, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val3a, NOT_FAILED, null);
    subscriber3.onNewResult(mSrc3);
    verifySubscriber(dataSource, mSrc3, ON_NEW_RESULT);
    verifyState(dataSource, mSrc3, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val3a, NOT_FAILED, null);

    Object val3b = mock(Object.class);
    setState(mSrc3, NOT_CLOSED, FINISHED, WITH_RESULT, val3b, NOT_FAILED, null);
    subscriber3.onNewResult(mSrc3);
    verifySubscriber(dataSource, mSrc3, ON_NEW_RESULT);
    verifyState(dataSource, mSrc3, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val3b, NOT_FAILED, null);

    Object val = mock(Object.class);
    setState(mSrc1, NOT_CLOSED, FINISHED, WITH_RESULT, val, NOT_FAILED, null);
    subscriber1.onNewResult(mSrc1);
    mInOrder.verify(mSrc3).close();
    verifySubscriber(dataSource, mSrc1, ON_NEW_RESULT);
    verifyState(dataSource, mSrc1, NOT_CLOSED, FINISHED, WITH_RESULT, val, NOT_FAILED, null);

    testClose(dataSource, mSrc1);
    verifyState(dataSource, null, CLOSED, FINISHED, WITHOUT_RESULT, null, NOT_FAILED, null);
  }

  /**
   * Immediate result of low-res data source followed by delayed result of the first data source.
   */
  @Test
  public void testLifecycle_ImmediateLowRes() {
    Object val2a = mock(Object.class);
    setState(mSrc2, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val2a, NOT_FAILED, null);
    respondOnSubscribe(mSrc2, ON_NEW_RESULT);

    DataSource<Object> dataSource = getAndSubscribe();
    DataSubscriber<Object> subscriber1 = verifyGetAndSubscribeM(mDataSourceSupplier1, mSrc1);
    DataSubscriber<Object> subscriber2 = verifyGetAndSubscribeM(mDataSourceSupplier2, mSrc2);

    verifySubscriber(dataSource, mSrc2, ON_NEW_RESULT);
    verifyState(dataSource, mSrc2, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val2a, NOT_FAILED, null);

    Object val2b = mock(Object.class);
    setState(mSrc2, NOT_CLOSED, FINISHED, WITH_RESULT, val2b, NOT_FAILED, null);
    subscriber2.onNewResult(mSrc2);
    verifySubscriber(dataSource, mSrc2, ON_NEW_RESULT);
    verifyState(dataSource, mSrc2, NOT_CLOSED, NOT_FINISHED, WITH_RESULT, val2b, NOT_FAILED, null);

    Object val = mock(Object.class);
    setState(mSrc1, NOT_CLOSED, FINISHED, WITH_RESULT, val, NOT_FAILED, null);
    subscriber1.onNewResult(mSrc1);
    mInOrder.verify(mSrc2).close();
    verifySubscriber(dataSource, mSrc1, ON_NEW_RESULT);
    verifyState(dataSource, mSrc1, NOT_CLOSED, FINISHED, WITH_RESULT, val, NOT_FAILED, null);

    testClose(dataSource, mSrc1);
    verifyState(dataSource, null, CLOSED, FINISHED, WITHOUT_RESULT, null, NOT_FAILED, null);
  }

  /**
   * Immediate finish of the first data source.
   */
  @Test
  public void testLifecycle_ImmediateFinish() {
    Object val = mock(Object.class);
    setState(mSrc1, NOT_CLOSED, FINISHED, WITH_RESULT, val, NOT_FAILED, null);
    respondOnSubscribe(mSrc1, ON_NEW_RESULT);

    DataSource<Object> dataSource = getAndSubscribe();
    DataSubscriber<Object> subscriber1 = verifyGetAndSubscribeM(mDataSourceSupplier1, mSrc1);

    verifySubscriber(dataSource, mSrc1, ON_NEW_RESULT);
    verifyState(dataSource, mSrc1, NOT_CLOSED, FINISHED, WITH_RESULT, val, NOT_FAILED, null);

    testClose(dataSource, mSrc1);
    verifyState(dataSource, null, CLOSED, FINISHED, WITHOUT_RESULT, null, NOT_FAILED, null);
  }
}

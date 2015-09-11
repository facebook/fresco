/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import java.io.Closeable;
import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;

import com.facebook.common.internal.Supplier;

import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.robolectric.*;
import org.robolectric.annotation.*;

import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest= Config.NONE)
public class StatefulProducerRunnableTest {

  private static final String REQUEST_ID = "awesomeRequestId";
  private static final String PRODUCER_NAME = "aBitLessAwesomeButStillAwesomeProducerName";

  @Mock public Consumer<Closeable> mConsumer;
  @Mock public ProducerListener mProducerListener;
  @Mock public Supplier<Closeable> mResultSupplier;
  @Mock public Closeable mResult;

  private RuntimeException mException;
  private Map<String, String> mSuccessMap;
  private Map<String, String> mFailureMap;
  private Map<String, String> mCancellationMap;

  private StatefulProducerRunnable<Closeable> mStatefulProducerRunnable;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    mException = new ConcurrentModificationException();
    mSuccessMap = new HashMap<>();
    mSuccessMap.put("state", "success");
    mFailureMap = new HashMap<>();
    mFailureMap.put("state", "failure");
    mCancellationMap = new HashMap<>();
    mCancellationMap.put("state", "cancelled");

    mStatefulProducerRunnable = new StatefulProducerRunnable<Closeable>(
        mConsumer,
        mProducerListener,
        PRODUCER_NAME,
        REQUEST_ID) {
      @Override
      protected void disposeResult(Closeable result) {
        try {
          result.close();
        } catch (IOException ioe) {
          throw new RuntimeException("unexpected IOException", ioe);
        }
      }

      @Override
      protected Closeable getResult() throws Exception {
        return mResultSupplier.get();
      }

      @Override
      protected Map<String, String> getExtraMapOnCancellation() {
        return mCancellationMap;
      }

      @Override
      protected Map<String, String> getExtraMapOnFailure(Exception exception) {
        return mFailureMap;
      }

      @Override
      protected Map<String, String> getExtraMapOnSuccess(Closeable result) {
        return mSuccessMap;
      }
    };
  }

  @Test
  public void testOnSuccess_extraMap() throws IOException {
    doReturn(true).when(mProducerListener).requiresExtraMap(REQUEST_ID);
    doReturn(mResult).when(mResultSupplier).get();
    mStatefulProducerRunnable.run();
    verify(mConsumer).onNewResult(mResult, true);
    verify(mProducerListener).onProducerStart(REQUEST_ID, PRODUCER_NAME);
    verify(mProducerListener).onProducerFinishWithSuccess(REQUEST_ID, PRODUCER_NAME, mSuccessMap);
    verify(mResult).close();
  }

  @Test
  public void testOnSuccess_noExtraMap() throws IOException {
    doReturn(mResult).when(mResultSupplier).get();
    mStatefulProducerRunnable.run();
    verify(mConsumer).onNewResult(mResult, true);
    verify(mProducerListener).onProducerStart(REQUEST_ID, PRODUCER_NAME);
    verify(mProducerListener).onProducerFinishWithSuccess(REQUEST_ID, PRODUCER_NAME, null);
    verify(mResult).close();
  }

  @Test
  public void testOnCancellation_extraMap() {
    doReturn(true).when(mProducerListener).requiresExtraMap(REQUEST_ID);
    mStatefulProducerRunnable.cancel();
    verify(mConsumer).onCancellation();
    verify(mProducerListener).onProducerStart(REQUEST_ID, PRODUCER_NAME);
    verify(mProducerListener).onProducerFinishWithCancellation(
        REQUEST_ID,
        PRODUCER_NAME,
        mCancellationMap);
  }

  @Test
  public void testOnCancellation_noExtraMap() {
    mStatefulProducerRunnable.cancel();
    verify(mConsumer).onCancellation();
    verify(mProducerListener).onProducerStart(REQUEST_ID, PRODUCER_NAME);
    verify(mProducerListener).onProducerFinishWithCancellation(REQUEST_ID, PRODUCER_NAME, null);
  }


  @Test
  public void testOnFailure_extraMap() {
    doReturn(true).when(mProducerListener).requiresExtraMap(REQUEST_ID);
    doThrow(mException).when(mResultSupplier).get();
    mStatefulProducerRunnable.run();
    verify(mConsumer).onFailure(mException);
    verify(mProducerListener).onProducerStart(REQUEST_ID, PRODUCER_NAME);
    verify(mProducerListener).onProducerFinishWithFailure(
        REQUEST_ID,
        PRODUCER_NAME,
        mException,
        mFailureMap);
  }

  @Test
  public void testOnFailure_noExtraMap() {
    doThrow(mException).when(mResultSupplier).get();
    mStatefulProducerRunnable.run();
    verify(mConsumer).onFailure(mException);
    verify(mProducerListener).onProducerStart(REQUEST_ID, PRODUCER_NAME);
    verify(mProducerListener).onProducerFinishWithFailure(
        REQUEST_ID,
        PRODUCER_NAME,
        mException,
        null);
  }
}

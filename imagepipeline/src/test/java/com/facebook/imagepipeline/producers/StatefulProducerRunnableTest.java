/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import static org.mockito.Mockito.*;

import com.facebook.common.internal.Supplier;
import java.io.Closeable;
import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.robolectric.*;
import org.robolectric.annotation.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class StatefulProducerRunnableTest {

  private static final String PRODUCER_NAME = "aBitLessAwesomeButStillAwesomeProducerName";

  @Mock public Consumer<Closeable> mConsumer;
  @Mock public ProducerListener2 mProducerListener;
  @Mock public Supplier<Closeable> mResultSupplier;
  @Mock public Closeable mResult;
  @Mock public ProducerContext mProducerContext;

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

    mStatefulProducerRunnable =
        new StatefulProducerRunnable<Closeable>(
            mConsumer, mProducerListener, mProducerContext, PRODUCER_NAME) {
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
    doReturn(true)
        .when(mProducerListener)
        .requiresExtraMap(eq(mProducerContext), eq(PRODUCER_NAME));
    doReturn(mResult).when(mResultSupplier).get();
    mStatefulProducerRunnable.run();
    verify(mConsumer).onNewResult(mResult, Consumer.IS_LAST);
    verify(mProducerListener).onProducerStart(mProducerContext, PRODUCER_NAME);
    verify(mProducerListener)
        .onProducerFinishWithSuccess(mProducerContext, PRODUCER_NAME, mSuccessMap);
    verify(mProducerListener, never())
        .onUltimateProducerReached(eq(mProducerContext), anyString(), anyBoolean());
    verify(mResult).close();
  }

  @Test
  public void testOnSuccess_noExtraMap() throws IOException {
    doReturn(mResult).when(mResultSupplier).get();
    mStatefulProducerRunnable.run();
    verify(mConsumer).onNewResult(mResult, Consumer.IS_LAST);
    verify(mProducerListener).onProducerStart(mProducerContext, PRODUCER_NAME);
    verify(mProducerListener).onProducerFinishWithSuccess(mProducerContext, PRODUCER_NAME, null);
    verify(mProducerListener, never())
        .onUltimateProducerReached(eq(mProducerContext), anyString(), anyBoolean());
    verify(mResult).close();
  }

  @Test
  public void testOnCancellation_extraMap() {
    doReturn(true).when(mProducerListener).requiresExtraMap(mProducerContext, PRODUCER_NAME);
    mStatefulProducerRunnable.cancel();
    verify(mConsumer).onCancellation();
    verify(mProducerListener).onProducerStart(mProducerContext, PRODUCER_NAME);
    verify(mProducerListener)
        .onProducerFinishWithCancellation(mProducerContext, PRODUCER_NAME, mCancellationMap);
    verify(mProducerListener, never())
        .onUltimateProducerReached(eq(mProducerContext), anyString(), anyBoolean());
  }

  @Test
  public void testOnCancellation_noExtraMap() {
    mStatefulProducerRunnable.cancel();
    verify(mConsumer).onCancellation();
    verify(mProducerListener).onProducerStart(mProducerContext, PRODUCER_NAME);
    verify(mProducerListener)
        .onProducerFinishWithCancellation(mProducerContext, PRODUCER_NAME, null);
    verify(mProducerListener, never())
        .onUltimateProducerReached(eq(mProducerContext), anyString(), anyBoolean());
  }

  @Test
  public void testOnFailure_extraMap() {
    doReturn(true).when(mProducerListener).requiresExtraMap(mProducerContext, PRODUCER_NAME);
    doThrow(mException).when(mResultSupplier).get();
    mStatefulProducerRunnable.run();
    verify(mConsumer).onFailure(mException);
    verify(mProducerListener).onProducerStart(mProducerContext, PRODUCER_NAME);
    verify(mProducerListener)
        .onProducerFinishWithFailure(mProducerContext, PRODUCER_NAME, mException, mFailureMap);
    verify(mProducerListener, never())
        .onUltimateProducerReached(eq(mProducerContext), anyString(), anyBoolean());
  }

  @Test
  public void testOnFailure_noExtraMap() {
    doThrow(mException).when(mResultSupplier).get();
    mStatefulProducerRunnable.run();
    verify(mConsumer).onFailure(mException);
    verify(mProducerListener).onProducerStart(mProducerContext, PRODUCER_NAME);
    verify(mProducerListener)
        .onProducerFinishWithFailure(mProducerContext, PRODUCER_NAME, mException, null);
    verify(mProducerListener, never())
        .onUltimateProducerReached(eq(mProducerContext), anyString(), anyBoolean());
  }
}

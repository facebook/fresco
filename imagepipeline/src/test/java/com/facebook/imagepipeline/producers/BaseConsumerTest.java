/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.robolectric.*;

import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class BaseConsumerTest {
  @Mock public Consumer mDelegatedConsumer;
  private Object mResult;
  private Object mResult2;
  private Exception mException;
  private BaseConsumer mBaseConsumer;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    mResult = new Object();
    mResult2 = new Object();
    mException = new RuntimeException();
    mBaseConsumer = new BaseConsumer() {
      @Override
      protected void onNewResultImpl(Object newResult, boolean isLast) {
        mDelegatedConsumer.onNewResult(newResult, isLast);
      }

      @Override
      protected void onFailureImpl(Throwable t) {
        mDelegatedConsumer.onFailure(t);
      }

      @Override
      protected void onCancellationImpl() {
        mDelegatedConsumer.onCancellation();
      }
    };
  }

  @Test
  public void testOnNewResultDoesNotThrow() {
    doThrow(new RuntimeException())
        .when(mDelegatedConsumer)
        .onNewResult(anyObject(), anyBoolean());
    mBaseConsumer.onNewResult(mResult, false);
    verify(mDelegatedConsumer).onNewResult(mResult, false);
  }

  @Test
  public void testOnFailureDoesNotThrow() {
    doThrow(new RuntimeException())
        .when(mDelegatedConsumer)
        .onFailure(any(Throwable.class));
    mBaseConsumer.onFailure(mException);
    verify(mDelegatedConsumer).onFailure(mException);
  }

  @Test
  public void testOnCancellationDoesNotThrow() {
    doThrow(new RuntimeException())
        .when(mDelegatedConsumer)
        .onCancellation();
    mBaseConsumer.onCancellation();
    verify(mDelegatedConsumer).onCancellation();
  }

  @Test
  public void testDoesNotForwardAfterFinalResult() {
    mBaseConsumer.onNewResult(mResult, true);
    mBaseConsumer.onFailure(mException);
    mBaseConsumer.onCancellation();
    verify(mDelegatedConsumer).onNewResult(mResult, true);
    verifyNoMoreInteractions(mDelegatedConsumer);
  }

  @Test
  public void testDoesNotForwardAfterOnFailure() {
    mBaseConsumer.onFailure(mException);
    mBaseConsumer.onNewResult(mResult, true);
    mBaseConsumer.onCancellation();
    verify(mDelegatedConsumer).onFailure(mException);
    verifyNoMoreInteractions(mDelegatedConsumer);
  }

  @Test
  public void testDoesNotForwardAfterOnCancellation() {
    mBaseConsumer.onCancellation();
    mBaseConsumer.onNewResult(mResult, true);
    mBaseConsumer.onFailure(mException);
    verify(mDelegatedConsumer).onCancellation();
    verifyNoMoreInteractions(mDelegatedConsumer);
  }

  @Test
  public void testDoesForwardAfterIntermediateResult() {
    mBaseConsumer.onNewResult(mResult, false);
    mBaseConsumer.onNewResult(mResult2, true);
    verify(mDelegatedConsumer).onNewResult(mResult2, true);
  }
}

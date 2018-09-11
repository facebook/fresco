/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.robolectric.*;

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
      protected void onNewResultImpl(Object newResult, @Status int status) {
        mDelegatedConsumer.onNewResult(newResult, status);
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
        .onNewResult(anyObject(), anyInt());
    mBaseConsumer.onNewResult(mResult, 0);
    verify(mDelegatedConsumer).onNewResult(mResult, 0);
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
    mBaseConsumer.onNewResult(mResult, Consumer.IS_LAST);
    mBaseConsumer.onFailure(mException);
    mBaseConsumer.onCancellation();
    verify(mDelegatedConsumer).onNewResult(mResult, Consumer.IS_LAST);
    verifyNoMoreInteractions(mDelegatedConsumer);
  }

  @Test
  public void testDoesNotForwardAfterOnFailure() {
    mBaseConsumer.onFailure(mException);
    mBaseConsumer.onNewResult(mResult, Consumer.IS_LAST);
    mBaseConsumer.onCancellation();
    verify(mDelegatedConsumer).onFailure(mException);
    verifyNoMoreInteractions(mDelegatedConsumer);
  }

  @Test
  public void testDoesNotForwardAfterOnCancellation() {
    mBaseConsumer.onCancellation();
    mBaseConsumer.onNewResult(mResult, Consumer.IS_LAST);
    mBaseConsumer.onFailure(mException);
    verify(mDelegatedConsumer).onCancellation();
    verifyNoMoreInteractions(mDelegatedConsumer);
  }

  @Test
  public void testDoesForwardAfterIntermediateResult() {
    mBaseConsumer.onNewResult(mResult, 0);
    mBaseConsumer.onNewResult(mResult2, Consumer.IS_LAST);
    verify(mDelegatedConsumer).onNewResult(mResult2, Consumer.IS_LAST);
  }

  @Test
  public void testIsLast() {
    assertThat(BaseConsumer.isLast(Consumer.IS_LAST)).isTrue();
    assertThat(BaseConsumer.isLast(Consumer.NO_FLAGS)).isFalse();
  }

  @Test
  public void testIsNotLast() {
    assertThat(BaseConsumer.isNotLast(Consumer.IS_LAST)).isFalse();
    assertThat(BaseConsumer.isNotLast(Consumer.NO_FLAGS)).isTrue();
  }

  @Test
  public void testTurnOnStatusFlag() {
    int turnedOn = BaseConsumer.turnOnStatusFlag(Consumer.NO_FLAGS, Consumer.IS_LAST);
    assertThat(BaseConsumer.isLast(turnedOn)).isTrue();
  }

  @Test
  public void testTurnOffStatusFlag() {
    int turnedOff = BaseConsumer.turnOffStatusFlag(Consumer.IS_LAST, Consumer.IS_LAST);
    assertThat(BaseConsumer.isNotLast(turnedOff)).isTrue();
  }

  @Test
  public void testStatusHasFlag() {
    assertThat(BaseConsumer
        .statusHasFlag(Consumer.IS_PLACEHOLDER | Consumer.IS_LAST, Consumer.IS_PLACEHOLDER))
        .isTrue();

    assertThat(BaseConsumer
        .statusHasFlag(Consumer.DO_NOT_CACHE_ENCODED | Consumer.IS_LAST, Consumer.IS_PLACEHOLDER))
        .isFalse();
  }

  @Test
  public void testStatusHasAnyFlag() {
    assertThat(BaseConsumer
        .statusHasAnyFlag(
            Consumer.IS_PLACEHOLDER | Consumer.IS_LAST,
            Consumer.IS_PLACEHOLDER | Consumer.DO_NOT_CACHE_ENCODED))
        .isTrue();

    assertThat(BaseConsumer
        .statusHasAnyFlag(
            Consumer.IS_PLACEHOLDER | Consumer.IS_LAST,
            Consumer.IS_PARTIAL_RESULT | Consumer.DO_NOT_CACHE_ENCODED))
        .isFalse();
  }
}

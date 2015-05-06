/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.common.executors;

import java.lang.reflect.Field;
import java.util.ConcurrentModificationException;
import java.util.concurrent.atomic.AtomicInteger;

import org.robolectric.RobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class StatefulRunnableTest {

  private StatefulRunnable mStatefulRunnable;
  private Object mResult;
  private ConcurrentModificationException mException;

  @Before
  public void setUp() throws Exception {
    mResult = new Object();
    mException = new ConcurrentModificationException();
    mStatefulRunnable = mock(StatefulRunnable.class, CALLS_REAL_METHODS);

    // setup state - no constructor has been run
    Field mStateField = StatefulRunnable.class.getDeclaredField("mState");
    mStateField.setAccessible(true);
    mStateField.set(mStatefulRunnable, new AtomicInteger(StatefulRunnable.STATE_CREATED));
    mStateField.setAccessible(false);
  }

  @Test
  public void testSuccess() throws Exception {
    runSuccess();
    InOrder inOrder = inOrder(mStatefulRunnable);
    inOrder.verify(mStatefulRunnable).onSuccess(mResult);
    inOrder.verify(mStatefulRunnable).disposeResult(mResult);
  }

  @Test
  public void testClosesResultWhenOnSuccessThrows() throws Exception {
    doThrow(mException).when(mStatefulRunnable).onSuccess(mResult);
    try {
      runSuccess();
      fail();
    } catch (ConcurrentModificationException cme) {
      // expected
    }
    verify(mStatefulRunnable).disposeResult(mResult);
  }

  @Test
  public void testFailure() throws Exception {
    runFailure();
    verify(mStatefulRunnable).onFailure(mException);
  }

  @Test
  public void testDoesNotRunAgainAfterStarted() throws Exception {
    mStatefulRunnable.mState.set(StatefulRunnable.STATE_STARTED);
    runSuccess();
    verify(mStatefulRunnable, never()).getResult();
  }

  @Test
  public void testCancellation() {
    mStatefulRunnable.cancel();
    verify(mStatefulRunnable).onCancellation();
  }

  @Test
  public void testDoesNotRunAfterCancellation() throws Exception {
    mStatefulRunnable.cancel();
    runSuccess();
    verify(mStatefulRunnable, never()).getResult();
  }

  @Test
  public void testDoesNotCancelTwice() {
    mStatefulRunnable.cancel();
    mStatefulRunnable.cancel();
    verify(mStatefulRunnable).onCancellation();
  }

  @Test
  public void testDoesNotCancelAfterStarted() {
    mStatefulRunnable.mState.set(StatefulRunnable.STATE_STARTED);
    mStatefulRunnable.cancel();
    verify(mStatefulRunnable, never()).onCancellation();
  }

  private void runSuccess() throws Exception {
    doReturn(mResult).when(mStatefulRunnable).getResult();
    mStatefulRunnable.run();
  }

  private void runFailure() throws Exception {
    doThrow(mException).when(mStatefulRunnable).getResult();
    mStatefulRunnable.run();
  }
}

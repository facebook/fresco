/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.common.executors;

import java.util.concurrent.Executor;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class SerialDelegatingExecutorTest {
  private SerialDelegatingExecutor mSerialDelegatingExecutor;
  private Executor mExecutor;
  private Runnable mRunnable;

  @Before
  public void setUp() {
    mExecutor = mock(Executor.class);
    mSerialDelegatingExecutor = new SerialDelegatingExecutor(mExecutor);
    mRunnable = mock(Runnable.class);
  }

  @Test
  public void testSubmitsTask() {
    mSerialDelegatingExecutor.execute(mRunnable);

    verify(mExecutor).execute(mSerialDelegatingExecutor.mRunnable);
  }

  @Test
  public void testExecutesTask() {
    mSerialDelegatingExecutor.execute(mRunnable);
    mSerialDelegatingExecutor.mRunnable.run();

    verify(mRunnable).run();
  }

  @Test
  public void testDoesNotSubmitMultipleRunnables() {
    mSerialDelegatingExecutor.execute(mRunnable);
    mSerialDelegatingExecutor.execute(mRunnable);

    verify(mExecutor).execute(mSerialDelegatingExecutor.mRunnable);
  }

  @Test
  public void testDoesSubmitNextRunnable() {
    mSerialDelegatingExecutor.execute(mRunnable);
    mSerialDelegatingExecutor.execute(mRunnable);
    mSerialDelegatingExecutor.mRunnable.run();

    verify(mExecutor, times(2)).execute(mSerialDelegatingExecutor.mRunnable);
  }

  @Test
  public void testExecutesMultipleTasks() {
    mSerialDelegatingExecutor.execute(mRunnable);
    mSerialDelegatingExecutor.execute(mRunnable);
    mSerialDelegatingExecutor.mRunnable.run();
    mSerialDelegatingExecutor.mRunnable.run();

    verify(mRunnable, times(2)).run();
  }

  @Test
  public void testDoesNotSubmitRunnableTooManyTimes() {
    mSerialDelegatingExecutor.execute(mRunnable);
    mSerialDelegatingExecutor.execute(mRunnable);
    mSerialDelegatingExecutor.mRunnable.run();
    mSerialDelegatingExecutor.mRunnable.run();

    verify(mExecutor, times(2)).execute(mSerialDelegatingExecutor.mRunnable);
  }
}

/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.executors;

import static org.assertj.core.api.Assertions.assertThat;

import android.os.Handler;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class HandlerExecutorServiceImplTest {

  private final AtomicInteger mCounter = new AtomicInteger();

  private HandlerExecutorServiceImpl mExecutorService;

  Runnable mIncrementCounterRunnable =
      new Runnable() {
        @Override
        public void run() {
          mCounter.incrementAndGet();
        }
      };

  @Before
  public void setup() {
    Handler handler = new Handler();
    mExecutorService = new HandlerExecutorServiceImpl(handler);
  }

  @Test
  public void testSimpleExecute() {
    mExecutorService.execute(mIncrementCounterRunnable);
    assertThat(mCounter.get()).isEqualTo(0);
    ShadowLooper.idleMainLooper();
    assertThat(mCounter.get()).isEqualTo(1);
  }

  @Test
  public void testDelay() {
    mExecutorService.schedule(mIncrementCounterRunnable, 30, TimeUnit.SECONDS);
    assertThat(mCounter.get()).isEqualTo(0);
    ShadowLooper.idleMainLooper(30, TimeUnit.SECONDS);
    assertThat(mCounter.get()).isEqualTo(1);
  }
}

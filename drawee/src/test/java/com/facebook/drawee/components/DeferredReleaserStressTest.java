/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.components;

import com.facebook.drawee.components.DeferredReleaser.Releasable;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
public class DeferredReleaserStressTest {
  private static final int K = 1000;

  @Test
  public void test() {
    stressTest(DeferredReleaser.getInstance());
  }

  private static void stressTest(final DeferredReleaser deferredReleaser) {
    final List<Releasable> releasables = new ArrayList<>();
    final AtomicInteger releaseCalls = new AtomicInteger(0);

    int batchSize = 32;
    int repeatCount = K;
    for (int i = 0; i < batchSize; i++) {
      releasables.add(new Noop(releaseCalls));
    }

    final List<Releasable> cancels = new ArrayList<>(releasables);
    Collections.shuffle(cancels);

    long durationScheduling = 0;
    long durationReleasing = 0;
    for (int i = 0; i < repeatCount; i++) {
      int before = releaseCalls.get();
      {
        ShadowLooper.pauseMainLooper();

        long start = System.nanoTime();
        // schedule then cancel
        for (Releasable releasable : releasables) {
          deferredReleaser.scheduleDeferredRelease(releasable);
        }

        for (Releasable releasable : cancels) {
          deferredReleaser.cancelDeferredRelease(releasable);
        }

        // finally schedule again
        for (Releasable releasable : releasables) {
          deferredReleaser.scheduleDeferredRelease(releasable);
        }
        durationScheduling += System.nanoTime() - start;

        Assert.assertEquals(before, releaseCalls.get());
      }

      {
        ShadowLooper.unPauseMainLooper();

        long start = System.nanoTime();
        ShadowLooper.runUiThreadTasks();
        durationReleasing += System.nanoTime() - start;

        Assert.assertEquals(before + batchSize, releaseCalls.get());
      }
    }

    Assert.assertEquals(repeatCount * batchSize, releaseCalls.get());

    NumberFormat numberFormat = NumberFormat.getInstance();
    System.out.println(
        deferredReleaser.getClass().getSimpleName()
            + " Schedule/Cancel: "
            + numberFormat.format(durationScheduling)
            + ", Release: "
            + numberFormat.format(durationReleasing));
  }

  private static class Noop implements Releasable {

    final AtomicInteger mReleaseCalls;

    public Noop(AtomicInteger releaseCalls) {
      mReleaseCalls = releaseCalls;
    }

    @Override
    public void release() {
      mReleaseCalls.incrementAndGet();
    }
  }
}

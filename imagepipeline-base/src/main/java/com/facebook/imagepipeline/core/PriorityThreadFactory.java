/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.core;

import android.os.Process;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ThreadFactory that applies a priority to the threads it creates.
 */
public class PriorityThreadFactory implements ThreadFactory {

  private final int mThreadPriority;
  private final String mPrefix;
  private final boolean mAddThreadNumber;

  private final AtomicInteger mThreadNumber = new AtomicInteger(1);

  /**
   * Creates a new PriorityThreadFactory with a given priority.
   *
   * <p>This value should be set to a value compatible with
   * {@link android.os.Process#setThreadPriority}, not {@link Thread#setPriority}.
   *
   */
  public PriorityThreadFactory(int threadPriority) {
    this(threadPriority, "PriorityThreadFactory", true);
  }

  public PriorityThreadFactory(int threadPriority, String prefix, boolean addThreadNumber) {
    mThreadPriority = threadPriority;
    this.mPrefix = prefix;
    this.mAddThreadNumber = addThreadNumber;
  }

  @Override
  public Thread newThread(final Runnable runnable) {
    Runnable wrapperRunnable = new Runnable() {
      @Override
      public void run() {
        try {
          Process.setThreadPriority(mThreadPriority);
        } catch (Throwable t) {
          // just to be safe
        }
        runnable.run();
      }
    };
    final String name;
    if (mAddThreadNumber) {
      name = mPrefix + "-" + mThreadNumber.getAndIncrement();
    } else {
      name = mPrefix;
    }
    return new Thread(wrapperRunnable, name);
  }

}

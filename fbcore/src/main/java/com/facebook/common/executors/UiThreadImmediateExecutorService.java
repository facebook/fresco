/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.common.executors;

import java.util.concurrent.ExecutorService;

import android.os.Handler;
import android.os.Looper;

/**
 * An {@link ExecutorService} that is backed by the application's main looper.
 *
 * <p/> If the execute is called from the thread of the application's main looper,
 * it will be executed synchronously.
 */
public class UiThreadImmediateExecutorService extends HandlerExecutorServiceImpl {
  private static UiThreadImmediateExecutorService sInstance = null;

  private UiThreadImmediateExecutorService() {
    super(new Handler(Looper.getMainLooper()));
  }

  public static UiThreadImmediateExecutorService getInstance() {
    if (sInstance == null) {
      sInstance = new UiThreadImmediateExecutorService();
    }
    return sInstance;
  }

  @Override
  public void execute(Runnable command) {
    if (isHandlerThread()) {
      command.run();
    } else {
      super.execute(command);
    }
  }
}

/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.executors;

import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.ExecutorService;
import javax.annotation.Nullable;

/**
 * An {@link ExecutorService} that is backed by the application's main looper.
 *
 * <p/> If the execute is called from the thread of the application's main looper,
 * it will be executed synchronously.
 */
public class UiThreadImmediateExecutorService extends HandlerExecutorServiceImpl {
  private static @Nullable UiThreadImmediateExecutorService sInstance = null;

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

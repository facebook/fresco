/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.common.executors;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;

/**
 * An {@link ExecutorService} that is backed by the application's main looper.
 */
public class UiThreadExecutorService extends HandlerExecutorServiceImpl {

  private static UiThreadExecutorService sInstance = null;

  private UiThreadExecutorService() {
    super(new Handler(Looper.getMainLooper()));
  }

  public static UiThreadExecutorService getInstance() {
    if (sInstance == null) {
      sInstance = new UiThreadExecutorService();
    }
    return sInstance;
  }
}

/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.executors;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * An {@link ExecutorService} that is backed by a handler.
 */
public interface HandlerExecutorService extends ScheduledExecutorService {

  /**
   * Quit the handler
   */
  void quit();

  /**
   * Check if we are currently in the handler thread of this HandlerExecutorService.
   */
  boolean isHandlerThread();
}

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

/**
 * The interface of the executors that execute tasks serially. The tasks submitted are executed
 * in FIFO order.
 */
public interface SerialExecutorService extends ExecutorService {
}

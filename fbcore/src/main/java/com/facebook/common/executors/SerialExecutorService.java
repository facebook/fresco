/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.executors;

import java.util.concurrent.ExecutorService;

/**
 * The interface of the executors that execute tasks serially. The tasks submitted are executed
 * in FIFO order.
 */
public interface SerialExecutorService extends ExecutorService {
}

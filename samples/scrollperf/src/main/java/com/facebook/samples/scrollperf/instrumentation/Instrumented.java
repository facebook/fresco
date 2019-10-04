/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.scrollperf.instrumentation;

/** Interface implemented by instrumented components */
public interface Instrumented {

  /**
   * Forget what happened so far and start measuring once again.
   *
   * @param tag String used to identify the image request
   * @param perfListener listener to be used to track the request state
   */
  void initInstrumentation(final String tag, PerfListener perfListener);
}

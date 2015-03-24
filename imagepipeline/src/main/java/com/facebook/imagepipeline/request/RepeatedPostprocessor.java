/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.request;

/**
 * Use an instance of this interface to perform post-process operations that must be performed
 * more than once.
 *
 * <p>Postprocessors are not supported on Gingerbread and below.
 */
public interface RepeatedPostprocessor extends Postprocessor {

  /**
   * Callback used to pass the postprocessor a reference to the object that will run the
   * postprocessor's {@code PostProcessor#process} method when the client requires.
   * @param runner
   */
  void setCallback(RepeatedPostprocessorRunner runner);
}

/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.request;

/**
 * Use an instance of this interface to perform post-process operations that must be performed
 * more than once.
 */
public interface RepeatedPostprocessor extends Postprocessor {

  /**
   * Callback used to pass the postprocessor a reference to the object that will run the
   * postprocessor's {@code PostProcessor#process} method when the client requires.
   * @param runner
   */
  void setCallback(RepeatedPostprocessorRunner runner);
}

/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.request;

/**
 * An instance of this class is used to run a postprocessor whenever the client requires.
 */
public interface RepeatedPostprocessorRunner {

  void update();
}

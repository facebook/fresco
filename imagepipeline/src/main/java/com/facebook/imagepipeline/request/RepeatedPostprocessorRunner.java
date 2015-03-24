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
 * An instance of this class is used to run a postprocessor whenever the client requires.
 */
public interface RepeatedPostprocessorRunner {

  void update();
}

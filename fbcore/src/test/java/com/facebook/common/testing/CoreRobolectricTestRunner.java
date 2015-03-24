/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.common.testing;

import org.junit.runners.model.InitializationError;
import org.robolectric.RobolectricTestRunner;

/**
 *
 *
 */
public class CoreRobolectricTestRunner extends RobolectricTestRunner {

  /**
   * Creates a runner to run {@code testClass}. Looks in your working directory for your
   * AndroidManifest.xml file and res directory by default. Use the {@link Config} annotation to
   * configure.
   *
   * @param testClass the test class to be run
   * @throws InitializationError if junit says so
   */
  public CoreRobolectricTestRunner(Class<?> testClass) throws InitializationError {
    super(testClass);
  }
}

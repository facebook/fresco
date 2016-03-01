/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package org.mockito.configuration;

import org.mockito.configuration.DefaultMockitoConfiguration;

/**
 * To build the imagepipeline-base module on gradle we need this class which must be in the
 * classpath and disable caching for classes. We need this to avoid ClassCastException related
 * to Mockito and PowerMockito
 */
public class MockitoConfiguration extends DefaultMockitoConfiguration {

  @Override
  public boolean enableClassCache() {
    return false;
  }
}

/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package org.mockito.configuration;


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

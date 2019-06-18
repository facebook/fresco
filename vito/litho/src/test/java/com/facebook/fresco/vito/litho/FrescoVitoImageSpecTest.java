/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.litho;

import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class FrescoVitoImageSpecTest extends BaseImageSpecTest {

  @Override
  Class getSpecClassName() {
    return FrescoVitoImageSpec.class;
  }
}

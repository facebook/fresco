// Copyright 2004-present Facebook. All Rights Reserved.
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

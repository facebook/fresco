/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.common;

import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;

public class BytesRangeTest {

  @Test
  public void testHeaderValueForRangeFrom() {
    assertThat(BytesRange.from(2000).toHttpRangeHeaderValue()).isEqualTo("bytes=2000-");
  }

  @Test
  public void testHeaderValueForRangeTo() {
    assertThat(BytesRange.toMax(1000).toHttpRangeHeaderValue()).isEqualTo("bytes=0-1000");
  }
}

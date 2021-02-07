/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.common;

import static org.fest.assertions.api.Assertions.assertThat;

import org.junit.Test;

public class ResizeOptionsTest {

  @Test
  public void testStaticConstructorWithValidWidthAndHeight() {
    ResizeOptions resizeOptions = ResizeOptions.forDimensions(1, 2);

    assertThat(resizeOptions).isNotNull();
    assertThat(resizeOptions.width).isEqualTo(1);
    assertThat(resizeOptions.height).isEqualTo(2);
  }

  @Test
  public void testStaticConstructorWithInvalidWidth() {
    ResizeOptions resizeOptions = ResizeOptions.forDimensions(0, 2);

    assertThat(resizeOptions).isNull();
  }

  @Test
  public void testStaticConstructorWithInvalidHeight() {
    ResizeOptions resizeOptions = ResizeOptions.forDimensions(1, 0);

    assertThat(resizeOptions).isNull();
  }

  @Test
  public void testStaticConstructorWithValidSquareSize() {
    ResizeOptions resizeOptions = ResizeOptions.forSquareSize(1);

    assertThat(resizeOptions).isNotNull();
    assertThat(resizeOptions.width).isEqualTo(1);
    assertThat(resizeOptions.height).isEqualTo(1);
  }

  @Test
  public void testStaticConstructorWithInvalidSquareSize() {
    ResizeOptions resizeOptions = ResizeOptions.forSquareSize(0);

    assertThat(resizeOptions).isNull();
  }
}

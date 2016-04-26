/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.common;

import java.util.Locale;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.util.HashCodeUtil;

/**
 * Options for resizing.
 *
 * <p> Describes the target bounds for the image (width, height) in pixels, as well as the
 * downscaling policy to employ.
 */
public class ResizeOptions {

  /* target width (in pixels) */
  public final int width;

  /* target height (in pixels) */
  public final int height;

  public ResizeOptions(
      int width,
      int height) {
    Preconditions.checkArgument(width > 0);
    Preconditions.checkArgument(height > 0);
    this.width = width;
    this.height = height;
  }

  @Override
  public int hashCode() {
    return HashCodeUtil.hashCode(
        width,
        height);
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if (!(other instanceof ResizeOptions)) {
      return false;
    }
    ResizeOptions that = (ResizeOptions) other;
    return this.width == that.width &&
        this.height == that.height;
  }

  @Override
  public String toString() {
    return String.format((Locale) null, "%dx%d", width, height);
  }
}

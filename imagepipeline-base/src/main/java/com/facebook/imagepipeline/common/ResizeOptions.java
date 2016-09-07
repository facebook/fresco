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
import com.facebook.imageutils.BitmapUtil;

/**
 * Options for resizing.
 *
 * <p> Describes the target bounds for the image (width, height) in pixels, as well as the
 * downscaling policy to employ.
 */
public class ResizeOptions {

  public static final float DEFAULT_ROUNDUP_FRACTION = 2.0f/3;

  /* target width (in pixels) */
  public final int width;

  /* target height (in pixels) */
  public final int height;

  /* max supported bitmap size (in pixels), defaults to BitmapUtil.MAX_BITMAP_SIZE */
  public final float maxBitmapSize;

  /* round-up fraction for resize process, defaults to DEFAULT_ROUNDUP_FRACTION */
  public final float roundUpFraction;

  public ResizeOptions(
      int width,
      int height) {
    this(width, height, BitmapUtil.MAX_BITMAP_SIZE);
  }

  public ResizeOptions(
      int width,
      int height,
      float maxBitmapSize) {
    this(width, height, maxBitmapSize, DEFAULT_ROUNDUP_FRACTION);
  }

  public ResizeOptions(
      int width,
      int height,
      float maxBitmapSize,
      float roundUpFraction) {
    Preconditions.checkArgument(width > 0);
    Preconditions.checkArgument(height > 0);
    this.width = width;
    this.height = height;
    this.maxBitmapSize = maxBitmapSize;
    this.roundUpFraction = roundUpFraction;
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

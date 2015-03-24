/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import com.facebook.imageformat.ImageFormat;

/**
 * Extracted meta data that is useful for image transforms.
 *
 * <p>Currently the data is useful for rotation and resize.
 */
public class ImageTransformMetaData {
  private final ImageFormat mImageFormat;
  private final int mRotationAngle;
  private final int mWidth;
  private final int mHeight;

  private ImageTransformMetaData(
      ImageFormat imageFormat,
      int rotationAngle,
      int width,
      int height) {
    mImageFormat = imageFormat;
    mRotationAngle = rotationAngle;
    mWidth = width;
    mHeight = height;
  }

  public ImageFormat getImageFormat() {
    return mImageFormat;
  }

  /**
   * Only valid if the image format is JPEG.
   * @return the rotation angle if the rotation angle is known, else -1. The rotation angle may not
   * be known if the image is incomplete (e.g. for progressive JPEGs).
   */
  public int getRotationAngle() {
    return mRotationAngle;
  }

  /**
   * Only valid if the image format is JPEG.
   * @return width if the width is known, else -1.
   */
  public int getWidth() {
    return mWidth;
  }

  /**
   * Only valid if the image format is JPEG.
   * @return height if the height is known, else -1.
   */
  public int getHeight() {
    return mHeight;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private ImageFormat mImageFormat;
    private int mRotationAngle;
    private int mWidth;
    private int mHeight;

    public Builder() {
      reset();
    }

    public Builder reset() {
      mImageFormat = ImageFormat.UNKNOWN;
      mRotationAngle = -1;
      mWidth = -1;
      mHeight = -1;
      return this;
    }

    public Builder setImageFormat(ImageFormat imageFormat) {
      mImageFormat = imageFormat;
      return this;
    }

    public Builder setRotationAngle(int rotationAngle) {
      mRotationAngle = rotationAngle;
      return this;
    }

    public Builder setWidth(int width) {
      mWidth = width;
      return this;
    }

    public Builder setHeight(int height) {
      mHeight = height;
      return this;
    }

    public ImageTransformMetaData build() {
      return new ImageTransformMetaData(mImageFormat, mRotationAngle, mWidth, mHeight);
    }
  }
}

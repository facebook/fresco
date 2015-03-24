/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.image;

/**
 * Interface containing information about an image.
 */
public interface ImageInfo {

  /**
   * @return width of the image
   */
  public int getWidth();

  /**
   * @return height of the image
   */
  public int getHeight();

  /**
   * @return quality information for the image
   */
  public QualityInfo getQualityInfo();
}

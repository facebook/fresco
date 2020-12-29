/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.image;

/** Interface containing information about an image. */
public interface ImageInfo extends HasImageMetadata {

  /** @return width of the image */
  int getWidth();

  /** @return height of the image */
  int getHeight();

  /** @return quality information for the image */
  QualityInfo getQualityInfo();
}

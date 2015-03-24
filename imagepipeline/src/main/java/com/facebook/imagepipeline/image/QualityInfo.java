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
 * Interface for image quality information
 */
public interface QualityInfo {

  /**
   * Used only to compare quality of two images that points to the same resource (uri).
   * <p> Higher number means higher quality.
   * <p> This is useful for caching in order to determine whether the new result is of higher
   * quality than what's already in the cache.
   */
  public int getQuality();

  /**
   * Whether the image is of good-enough quality.
   * <p> When fetching image progressively, the few first results can be of really poor quality,
   * but eventually, they get really close to original image, and we mark those as good-enough.
   */
  public boolean isOfGoodEnoughQuality();

  /**
   * Whether the image is of full quality.
   * <p> For progressive JPEGs, this is the final scan. For other image types, this is always true.
   */
  public boolean isOfFullQuality();

}

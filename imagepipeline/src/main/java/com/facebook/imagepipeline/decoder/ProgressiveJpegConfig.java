/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.decoder;

import com.facebook.imagepipeline.image.QualityInfo;
import com.facebook.imagepipeline.request.ImageRequest;

/** Progressive JPEG config. */
public interface ProgressiveJpegConfig {

  /** Default minimum time between partial results, in milliseconds. */
  long DEFAULT_TIME_BETWEEN_PARTIAL_RESULTS_MS = 100;

  /** Shortcut for checking if we should attempt to decode progressively. */
  boolean decodeProgressively(ImageRequest imageRequest);

  /** Gets the next scan-number that should be decoded after the given scan-number. */
  int getNextScanNumberToDecode(ImageRequest imageRequest, int scanNumber);

  /** Gets the quality information for the given scan-number. */
  QualityInfo getQualityInfo(ImageRequest imageRequest, int scanNumber);

  /**
   * Gets the minimum time between two consecutive partial results being propagated upstream, in
   * milliseconds.
   */
  default long getTimeBetweenPartialResultsMs(ImageRequest imageRequest) {
    return DEFAULT_TIME_BETWEEN_PARTIAL_RESULTS_MS;
  }
}

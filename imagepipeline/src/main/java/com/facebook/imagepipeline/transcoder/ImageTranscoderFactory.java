/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.transcoder;

public interface ImageTranscoderFactory {

  /**
   * Creates an {@link ImageTranscoder} that enables or disables resizing depending on {@code
   * isResizingEnabled}
   *
   * @param isResizingEnabled true if resizing is allowed.
   * @return The {@link ImageTranscoder}
   */
  ImageTranscoder createImageTranscoder(boolean isResizingEnabled);
}

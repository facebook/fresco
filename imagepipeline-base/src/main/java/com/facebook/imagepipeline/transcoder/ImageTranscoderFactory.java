/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.transcoder;

import com.facebook.imageformat.ImageFormat;
import javax.annotation.Nullable;

public interface ImageTranscoderFactory {

  /**
   * Creates an {@link ImageTranscoder} that enables or disables resizing depending on {@code
   * isResizingEnabled}. It can return null if the {@link ImageFormat} is not supported by this
   * {@link ImageTranscoder}.
   *
   * <p>Note that if JPEG images are not supported, we will fallback to our native {@link
   * ImageTranscoder} implementation.
   *
   * @param imageFormat the {@link ImageFormat} of the input images.
   * @param isResizingEnabled true if resizing is allowed.
   * @return The {@link ImageTranscoder} or null if the image format is not supported.
   */
  @Nullable
  ImageTranscoder createImageTranscoder(ImageFormat imageFormat, boolean isResizingEnabled);
}

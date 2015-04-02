/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.common;

import javax.annotation.concurrent.Immutable;

/**
 * Options for changing the behavior of the {@code ImageDecoder}.
 */
@Immutable
public class ImageDecodeOptions {

  private static final ImageDecodeOptions DEFAULTS = ImageDecodeOptions.newBuilder().build();

  /**
   * Decoding of intermediate results for an image won't happen more often that minDecodeIntervalMs.
   */
  public final int minDecodeIntervalMs;

  /**
   * Background color used when converting to image formats that don't support transparency.
   */
  public final int backgroundColor;

  /**
   * Forces use of the old animation drawable code that we're in process of deprecating.
   */
  public final boolean forceOldAnimationCode;

  /**
   * Whether to decode a preview frame for animated images.
   */
  public final boolean decodePreviewFrame;

  /**
   * Indicates that the last frame should be used as the preview frame instead of the first.
   */
  public final boolean useLastFrameForPreview;

  /**
   * Whether to decode all the frames and store them in memory. This should only ever be used
   * for animations that are known to be small (e.g. stickers). Caching dozens of large Bitmaps
   * in memory for general GIFs or WebP's will not fit in memory.
   */
  public final boolean decodeAllFrames;

  ImageDecodeOptions(ImageDecodeOptionsBuilder b) {
    this.minDecodeIntervalMs = b.getMinDecodeIntervalMs();
    this.backgroundColor = b.getBackgroundColor();
    this.forceOldAnimationCode = b.getForceOldAnimationCode();
    this.decodePreviewFrame = b.getDecodePreviewFrame();
    this.useLastFrameForPreview = b.getUseLastFrameForPreview();
    this.decodeAllFrames = b.getDecodeAllFrames();
  }

  /**
   * Gets the default options.
   *
   * @return  the default options
   */
  public static ImageDecodeOptions defaults() {
    return DEFAULTS;
  }

  /**
   * Creates a new builder.
   *
   * @return a new builder
   */
  public static ImageDecodeOptionsBuilder newBuilder() {
    return new ImageDecodeOptionsBuilder();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ImageDecodeOptions that = (ImageDecodeOptions) o;

    if (backgroundColor != that.backgroundColor) return false;
    if (forceOldAnimationCode != that.forceOldAnimationCode) return false;
    if (decodePreviewFrame != that.decodePreviewFrame) return false;
    if (useLastFrameForPreview != that.useLastFrameForPreview) return false;
    if (decodeAllFrames != that.decodeAllFrames) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = backgroundColor;
    result = 31 * result + (forceOldAnimationCode ? 1 : 0);
    return result;
  }
}

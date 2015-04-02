/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.common;

/**
 * Builder for {@link ImageDecodeOptions}.
 */
public class ImageDecodeOptionsBuilder {

  private int mMinDecodeIntervalMs = 100;
  private int mBackgroundColor = 0xFFFFFF;
  private boolean mForceOldAnimationCode;
  private boolean mDecodePreviewFrame;
  private boolean mUseLastFrameForPreview;
  private boolean mDecodeAllFrames;

  ImageDecodeOptionsBuilder() {
  }

  /**
   * Sets the builder to be equivalent to the specified options.
   *
   * @param options the options to copy from
   * @return this builder
   */
  public ImageDecodeOptionsBuilder setFrom(ImageDecodeOptions options) {
    mBackgroundColor = options.backgroundColor;
    mForceOldAnimationCode = options.forceOldAnimationCode;
    mDecodePreviewFrame = options.decodePreviewFrame;
    mUseLastFrameForPreview = options.useLastFrameForPreview;
    mDecodeAllFrames = options.decodeAllFrames;
    return this;
  }

  /**
   * Sets the minimum decode interval.
   *
   * <p/> Decoding of intermediate results won't happen more often that intervalMs. If another
   * intermediate result comes too soon, it will be decoded only after intervalMs since the last
   * decode. If there were more intermediate results in between, only the last one gets decoded.
   * @param intervalMs the minimum decode interval in milliseconds
   * @return this builder
   */
  public ImageDecodeOptionsBuilder setMinDecodeIntervalMs(int intervalMs) {
    mMinDecodeIntervalMs = intervalMs;
    return this;
  }

  /**
   * Gets the minimum decode interval.
   *
   * @return the minimum decode interval in milliseconds
   */
  public int getMinDecodeIntervalMs() {
    return mMinDecodeIntervalMs;
  }

  /**
   * Sets the background color used when converting to image formats that don't support
   * transparency.
   *
   * @param backgroundColor the background color to use
   * @return this builder
   */
  public ImageDecodeOptionsBuilder setBackgroundColor(int backgroundColor) {
    mBackgroundColor = backgroundColor;
    return this;
  }

  /**
   * Gets the background color used when converting to image formats that don't support
   * transparency.
   *
   * @return the background color to use
   */
  public int getBackgroundColor() {
    return mBackgroundColor;
  }

  /**
   * Sets whether to force use of the old animation drawable code that we're in process of
   * deprecating.
   *
   * @param forceOldAnimationCode whether to force use of the old animation drawable code
   * @return this builder
   */
  public ImageDecodeOptionsBuilder setForceOldAnimationCode(boolean forceOldAnimationCode) {
    mForceOldAnimationCode = forceOldAnimationCode;
    return this;
  }

  /**
   * Gets whether to force use of the old animation drawable code that we're in process of
   * deprecating.
   *
   * @return whether to force use of the old animation drawable code
   */
  public boolean getForceOldAnimationCode() {
    return mForceOldAnimationCode;
  }

  /**
   * Sets whether to decode a preview frame for animated images.
   *
   * @param decodePreviewFrame whether to decode a preview frame
   * @return this builder
   */
  public ImageDecodeOptionsBuilder setDecodePreviewFrame(boolean decodePreviewFrame) {
    mDecodePreviewFrame = decodePreviewFrame;
    return this;
  }

  /**
   * Gets whether to decode a preview frame for animated images.
   *
   * @return whether to decode a preview frame
   */
  public boolean getDecodePreviewFrame() {
    return mDecodePreviewFrame;
  }

  /**
   * Gets whether to use the last frame for the preview image (defaults to the first frame).
   *
   * @return whether to use the last frame for the preview image
   */
  public boolean getUseLastFrameForPreview() {
    return mUseLastFrameForPreview;
  }

  /**
   * Sets whether to use the last frame for the preview image (defaults to the first frame).
   *
   * @param useLastFrameForPreview whether to use the last frame for the preview image
   * @return this builder
   */
  public ImageDecodeOptionsBuilder setUseLastFrameForPreview(boolean useLastFrameForPreview) {
    mUseLastFrameForPreview = useLastFrameForPreview;
    return this;
  }

  /**
   * Gets whether to decode all the frames and store them in memory. This should only ever be used
   * for animations that are known to be small (e.g. stickers). Caching dozens of large Bitmaps
   * in memory for general GIFs or WebP's will not fit in memory.
   *
   * @return whether to decode all the frames and store them in memory
   */
  public boolean getDecodeAllFrames() {
    return mDecodeAllFrames;
  }

  /**
   * Sets whether to decode all the frames and store them in memory. This should only ever be used
   * for animations that are known to be small (e.g. stickers). Caching dozens of large Bitmaps
   * in memory for general GIFs or WebP's will not fit in memory.
   *
   * @param decodeAllFrames whether to decode all the frames and store them in memory
   * @return this builder
   */
  public ImageDecodeOptionsBuilder setDecodeAllFrames(boolean decodeAllFrames) {
    mDecodeAllFrames = decodeAllFrames;
    return this;
  }

  /**
   * Builds the immutable {@link ImageDecodeOptions} instance.
   *
   * @return the immutable instance
   */
  public ImageDecodeOptions build() {
    return new ImageDecodeOptions(this);
  }
}

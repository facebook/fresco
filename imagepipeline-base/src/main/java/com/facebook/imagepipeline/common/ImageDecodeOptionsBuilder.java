/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.common;

import javax.annotation.Nullable;

import android.graphics.Bitmap;

import com.facebook.imagepipeline.decoder.ImageDecoder;

/**
 * Builder for {@link ImageDecodeOptions}.
 */
public class ImageDecodeOptionsBuilder {

  private int mMinDecodeIntervalMs = 100;
  private boolean mDecodePreviewFrame;
  private boolean mUseLastFrameForPreview;
  private boolean mDecodeAllFrames;
  private boolean mForceStaticImage;
  private Bitmap.Config mBitmapConfig = Bitmap.Config.ARGB_8888;
  private @Nullable ImageDecoder mCustomImageDecoder;

  public ImageDecodeOptionsBuilder() {
  }

  /**
   * Sets the builder to be equivalent to the specified options.
   *
   * @param options the options to copy from
   * @return this builder
   */
  public ImageDecodeOptionsBuilder setFrom(ImageDecodeOptions options) {
    mDecodePreviewFrame = options.decodePreviewFrame;
    mUseLastFrameForPreview = options.useLastFrameForPreview;
    mDecodeAllFrames = options.decodeAllFrames;
    mForceStaticImage = options.forceStaticImage;
    mBitmapConfig = options.bitmapConfig;
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
   * Sets whether to force animated image formats to be decoded as static, non-animated images.
   *
   * @param forceStaticImage whether to force the image to be decoded as a static image
   * @return this builder
   */
  public ImageDecodeOptionsBuilder setForceStaticImage(boolean forceStaticImage) {
    mForceStaticImage = forceStaticImage;
    return this;
  }

  /**
   * Set a custom image decoder override to be used for the given image.
   * This will bypass all default decoders and only use the provided custom image decoder
   * for the given image.
   *
   * @param customImageDecoder the custom decoder to use
   * @return this builder
   */
  public ImageDecodeOptionsBuilder setCustomImageDecoder(
      @Nullable ImageDecoder customImageDecoder) {
    mCustomImageDecoder = customImageDecoder;
    return this;
  }

  /**
   * Get the custom image decoder, if one has been set.
   *
   * @return the custom image decoder or null if not set
   */
  @Nullable
  public ImageDecoder getCustomImageDecoder() {
    return mCustomImageDecoder;
  }

  /**
   * Gets whether to force animated image formats to be decoded as static, non-animated images.
   *
   * @return whether to force animated image formats to be decoded as static
   */
  public boolean getForceStaticImage() {
    return mForceStaticImage;
  }

  /**
   * Gets which config image will be decode with;
   *
   * @return which config image will be decode with
   */
  public Bitmap.Config getBitmapConfig() {
    return mBitmapConfig;
  }

  /**
   * Sets which config static image will be decode with;
   * @param bitmapConfig which config static image will be decode with;
   */
  public ImageDecodeOptionsBuilder setBitmapConfig(Bitmap.Config bitmapConfig) {
    mBitmapConfig = bitmapConfig;
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

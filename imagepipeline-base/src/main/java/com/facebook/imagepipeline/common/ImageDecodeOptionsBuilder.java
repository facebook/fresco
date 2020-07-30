/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.common;

import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import com.facebook.imagepipeline.decoder.ImageDecoder;
import com.facebook.imagepipeline.transformation.BitmapTransformation;
import javax.annotation.Nullable;

/** Builder for {@link ImageDecodeOptions}. */
public class ImageDecodeOptionsBuilder<T extends ImageDecodeOptionsBuilder> {

  private int mMinDecodeIntervalMs = 100;
  private int mMaxDimensionPx = Integer.MAX_VALUE;
  private boolean mDecodePreviewFrame;
  private boolean mUseLastFrameForPreview;
  private boolean mDecodeAllFrames;
  private boolean mForceStaticImage;
  private Bitmap.Config mBitmapConfig = Bitmap.Config.ARGB_8888;
  private @Nullable ImageDecoder mCustomImageDecoder;
  private @Nullable BitmapTransformation mBitmapTransformation;
  private @Nullable ColorSpace mColorSpace;
  private boolean mExcludeBitmapConfigFromComparison;

  public ImageDecodeOptionsBuilder() {}

  /**
   * Sets the builder to be equivalent to the specified options.
   *
   * @param options the options to copy from
   * @return this builder
   */
  public ImageDecodeOptionsBuilder setFrom(ImageDecodeOptions options) {
    mMinDecodeIntervalMs = options.minDecodeIntervalMs;
    mMaxDimensionPx = options.maxDimensionPx;
    mDecodePreviewFrame = options.decodePreviewFrame;
    mUseLastFrameForPreview = options.useLastFrameForPreview;
    mDecodeAllFrames = options.decodeAllFrames;
    mForceStaticImage = options.forceStaticImage;
    mBitmapConfig = options.bitmapConfig;
    mCustomImageDecoder = options.customImageDecoder;
    mBitmapTransformation = options.bitmapTransformation;
    mColorSpace = options.colorSpace;
    return getThis();
  }

  /**
   * Sets the minimum decode interval.
   *
   * <p>Decoding of intermediate results won't happen more often that intervalMs. If another
   * intermediate result comes too soon, it will be decoded only after intervalMs since the last
   * decode. If there were more intermediate results in between, only the last one gets decoded.
   *
   * @param intervalMs the minimum decode interval in milliseconds
   * @return this builder
   */
  public T setMinDecodeIntervalMs(int intervalMs) {
    mMinDecodeIntervalMs = intervalMs;
    return getThis();
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
   * Sets the maximum image dimension (width or height).
   *
   * @param maxDimensionPx the maximum image dimension in pixels
   * @return this builder
   */
  public T setMaxDimensionPx(int maxDimensionPx) {
    mMaxDimensionPx = maxDimensionPx;
    return getThis();
  }

  /**
   * Gets the maximum image dimension (width or height).
   *
   * @return the maxinum image dimension in pixels
   */
  public int getMaxDimensionPx() {
    return mMaxDimensionPx;
  }

  /**
   * Sets whether to decode a preview frame for animated images.
   *
   * @param decodePreviewFrame whether to decode a preview frame
   * @return this builder
   */
  public T setDecodePreviewFrame(boolean decodePreviewFrame) {
    mDecodePreviewFrame = decodePreviewFrame;
    return getThis();
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
  public T setUseLastFrameForPreview(boolean useLastFrameForPreview) {
    mUseLastFrameForPreview = useLastFrameForPreview;
    return getThis();
  }

  /**
   * Gets whether to decode all the frames and store them in memory. This should only ever be used
   * for animations that are known to be small (e.g. stickers). Caching dozens of large Bitmaps in
   * memory for general GIFs or WebP's will not fit in memory.
   *
   * @return whether to decode all the frames and store them in memory
   */
  public boolean getDecodeAllFrames() {
    return mDecodeAllFrames;
  }

  /**
   * Sets whether to decode all the frames and store them in memory. This should only ever be used
   * for animations that are known to be small (e.g. stickers). Caching dozens of large Bitmaps in
   * memory for general GIFs or WebP's will not fit in memory.
   *
   * @param decodeAllFrames whether to decode all the frames and store them in memory
   * @return this builder
   */
  public T setDecodeAllFrames(boolean decodeAllFrames) {
    mDecodeAllFrames = decodeAllFrames;
    return getThis();
  }

  /**
   * Sets whether to force animated image formats to be decoded as static, non-animated images.
   *
   * @param forceStaticImage whether to force the image to be decoded as a static image
   * @return this builder
   */
  public T setForceStaticImage(boolean forceStaticImage) {
    mForceStaticImage = forceStaticImage;
    return getThis();
  }

  /**
   * Set a custom image decoder override to be used for the given image. This will bypass all
   * default decoders and only use the provided custom image decoder for the given image.
   *
   * @param customImageDecoder the custom decoder to use
   * @return this builder
   */
  public T setCustomImageDecoder(@Nullable ImageDecoder customImageDecoder) {
    mCustomImageDecoder = customImageDecoder;
    return getThis();
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
   *
   * @param bitmapConfig which config static image will be decode with;
   */
  public T setBitmapConfig(Bitmap.Config bitmapConfig) {
    mBitmapConfig = bitmapConfig;
    return getThis();
  }

  /**
   * Set a custom in-place bitmap transformation that is applied immediately after decoding.
   *
   * @param bitmapTransformation the transformation to use
   * @return the builder
   */
  public T setBitmapTransformation(@Nullable BitmapTransformation bitmapTransformation) {
    mBitmapTransformation = bitmapTransformation;
    return getThis();
  }

  @Nullable
  public BitmapTransformation getBitmapTransformation() {
    return mBitmapTransformation;
  }

  /**
   * Sets the target color space for decoding. When possible, the color space transformation will be
   * performed at load time. This requires SDK version >= 26, otherwise it's a no-op.
   *
   * @param colorSpace target color space for decoding.
   */
  public T setColorSpace(ColorSpace colorSpace) {
    mColorSpace = colorSpace;
    return getThis();
  }

  /**
   * Gets the target color space for decoding.
   *
   * @return the target color space.
   */
  @Nullable
  public ColorSpace getColorSpace() {
    return mColorSpace;
  }

  public T setExcludeBitmapConfigFromComparison(boolean excludeBitmapConfigFromComparison) {
    mExcludeBitmapConfigFromComparison = excludeBitmapConfigFromComparison;
    return getThis();
  }

  public boolean getExcludeBitmapConfigFromComparison() {
    return mExcludeBitmapConfigFromComparison;
  }

  /**
   * Builds the immutable {@link ImageDecodeOptions} instance.
   *
   * @return the immutable instance
   */
  public ImageDecodeOptions build() {
    return new ImageDecodeOptions(this);
  }

  protected T getThis() {
    return (T) this;
  }
}

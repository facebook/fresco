/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.common;

import android.graphics.Bitmap;
import com.facebook.imagepipeline.decoder.ImageDecoder;
import com.facebook.imagepipeline.transformation.BitmapTransformation;
import java.util.Locale;
import javax.annotation.Nullable;
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

  /**
   * Force image to be rendered as a static image, even if it is an animated format.
   *
   * This flag will force animated GIFs to be rendered as static images
   */
  public final boolean forceStaticImage;

  /**
   * Allow color space transformation to sRGB. This flag can affect performance, unless the color
   * space is unknown or if the color space is already sRGB.
   */
  public final boolean transformToSRGB;

  /**
   * StaticImage and JPEG will decode with this config;
   */
  public final Bitmap.Config bitmapConfig;

  /**
   * Custom image decoder override.
   */
  public final @Nullable ImageDecoder customImageDecoder;

  /** Bitmap transformation override */
  public final @Nullable BitmapTransformation bitmapTransformation;

  public ImageDecodeOptions(ImageDecodeOptionsBuilder b) {
    this.minDecodeIntervalMs = b.getMinDecodeIntervalMs();
    this.decodePreviewFrame = b.getDecodePreviewFrame();
    this.useLastFrameForPreview = b.getUseLastFrameForPreview();
    this.decodeAllFrames = b.getDecodeAllFrames();
    this.forceStaticImage = b.getForceStaticImage();
    this.bitmapConfig = b.getBitmapConfig();
    this.customImageDecoder = b.getCustomImageDecoder();
    this.transformToSRGB = b.getTransformToSRGB();
    this.bitmapTransformation = b.getBitmapTransformation();
  }

  /**
   * Gets the default options.
   *
   * @return the default options
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

    if (decodePreviewFrame != that.decodePreviewFrame) return false;
    if (useLastFrameForPreview != that.useLastFrameForPreview) return false;
    if (decodeAllFrames != that.decodeAllFrames) return false;
    if (forceStaticImage != that.forceStaticImage) return false;
    if (transformToSRGB != that.transformToSRGB) return false;
    if (bitmapConfig != that.bitmapConfig) return false;
    if (customImageDecoder != that.customImageDecoder) return false;
    if (bitmapTransformation != that.bitmapTransformation) return false;
    return true;
  }

  @Override
  public int hashCode() {
    int result = minDecodeIntervalMs;
    result = 31 * result + (decodePreviewFrame ? 1 : 0);
    result = 31 * result + (useLastFrameForPreview ? 1 : 0);
    result = 31 * result + (decodeAllFrames ? 1 : 0);
    result = 31 * result + (forceStaticImage ? 1 : 0);
    result = 31 * result + (transformToSRGB ? 1 : 0);
    result = 31 * result + bitmapConfig.ordinal();
    result = 31 * result + (customImageDecoder != null ? customImageDecoder.hashCode() : 0);
    result = 31 * result + (bitmapTransformation != null ? bitmapTransformation.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return String.format(
        (Locale) null,
        "%d-%b-%b-%b-%b-%b-%s-%s-%s",
        minDecodeIntervalMs,
        decodePreviewFrame,
        useLastFrameForPreview,
        decodeAllFrames,
        forceStaticImage,
        transformToSRGB,
        bitmapConfig.name(),
        customImageDecoder,
        bitmapTransformation);
  }
}
